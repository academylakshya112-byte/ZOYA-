package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ZoyaAccessibilityService : AccessibilityService() {

    companion object {
        var shouldAutoClick = false
            set(value) {
                field = value
                if (value) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        field = false
                    }, 15000) // 15 seconds to handle loading/flows
                }
            }
        var targetAppName = "whatsapp"
        var instance: ZoyaAccessibilityService? = null

        // Notification storage
        var lastNotificationSender: String? = null
        var lastNotificationText: String? = null
        var lastNotificationPackage: String? = null
        var lastNotificationTime: Long = 0L

        // WhatsApp automation state
        var targetContactToMessage: String? = null
        var messageToBhej: String? = null
        var whatsappMode: String? = null // "message" or "call"
        var automationStep: Int = 0 // 0: Idle, 1: Initiating search, 2: Typing name & selecting, 3: Chat screen opened

        fun startWhatsAppAutomation(contactName: String, message: String?, mode: String): String {
            val inst = instance ?: return "Error: Zoya Automation Accessibility Service is not running. Please enable Zoya in Settings > Accessibility."
            targetContactToMessage = contactName
            messageToBhej = message
            whatsappMode = mode
            automationStep = 1
            shouldAutoClick = true
            
            val pm = inst.packageManager
            val intent = pm.getLaunchIntentForPackage("com.whatsapp") ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                inst.startActivity(intent)
                return "Opening WhatsApp to $mode $contactName..."
            } else {
                return "WhatsApp application is not installed on this device."
            }
        }

        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val inst = instance ?: return false
            val path = android.graphics.Path()
            path.moveTo(x, y)
            path.lineTo(x, y)

            val builder = android.accessibilityservice.GestureDescription.Builder()
            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 50))
            val gesture = builder.build()

            return inst.dispatchGesture(gesture, null, null)
        }

        fun clickTextOnScreen(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val nodes = root.findAccessibilityNodeInfosByText(text)
            for (node in nodes) {
                var current: AccessibilityNodeInfo? = node
                while(current != null) {
                    val bounds = android.graphics.Rect()
                    current.getBoundsInScreen(bounds)
                    if (!bounds.isEmpty && (current.isClickable || current == node)) {
                        val x = bounds.centerX().toFloat()
                        val y = bounds.centerY().toFloat()
                        if (dispatchGestureClick(x, y)) return true
                    }
                    current = current.parent
                }
            }
            return false
        }

        fun getScreenText(): String {
            val inst = instance ?: return "Error: Zoya Automation Accessibility Service is not running. Please enable Zoya Automation in Settings > Accessibility."
            val root = inst.rootInActiveWindow ?: return "Error: Could not retrieve the active window. Please make sure the screen is on and the app is visible."
            val packageName = root.packageName?.toString() ?: "Unknown"
            val sb = StringBuilder()
            sb.append("Current Foreground App: $packageName\n")
            sb.append("Visible Screen Elements:\n")
            val seenText = mutableSetOf<String>()
            traverseAndExtractText(root, sb, seenText)
            val result = sb.toString().trim()
            return if (seenText.isEmpty()) "Screen has no visible or readable text currently." else result
        }

        fun scrollScreen(direction: String): Boolean {
            val inst = instance ?: return false
            val resources = inst.resources
            val dm = resources.displayMetrics
            val width = dm.widthPixels.toFloat()
            val height = dm.heightPixels.toFloat()

            val startX: Float
            val startY: Float
            val endX: Float
            val endY: Float

            when (direction.lowercase()) {
                "down" -> {
                    // Swipes up to scroll down
                    startX = width / 2f
                    startY = height * 0.8f
                    endX = width / 2f
                    endY = height * 0.2f
                }
                "up" -> {
                    // Swipes down to scroll up
                    startX = width / 2f
                    startY = height * 0.2f
                    endX = width / 2f
                    endY = height * 0.8f
                }
                "left" -> {
                    // Swipes right to scroll left
                    startX = width * 0.2f
                    startY = height / 2f
                    endX = width * 0.8f
                    endY = height / 2f
                }
                "right" -> {
                    // Swipes left to scroll right
                    startX = width * 0.8f
                    startY = height / 2f
                    endX = width * 0.2f
                    endY = height / 2f
                }
                else -> return false
            }

            val path = android.graphics.Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)

            val builder = android.accessibilityservice.GestureDescription.Builder()
            builder.addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 300))
            val gesture = builder.build()

            return inst.dispatchGesture(gesture, null, null)
        }

        private fun traverseAndExtractText(node: AccessibilityNodeInfo?, sb: StringBuilder, seenText: MutableSet<String>) {
            if (node == null) return
            
            val text = node.text?.toString()?.trim()
            val desc = node.contentDescription?.toString()?.trim()
            val className = node.className?.toString() ?: ""
            val isClickable = node.isClickable
            
            val visibleText = when {
                !text.isNullOrBlank() -> text
                !desc.isNullOrBlank() -> desc
                else -> null
            }
            
            if (visibleText != null && !seenText.contains(visibleText)) {
                seenText.add(visibleText)
                if (isClickable || className.contains("Button", ignoreCase = true) || className.contains("Image", ignoreCase = true)) {
                    sb.append("- [Button/Clickable] $visibleText\n")
                } else {
                    sb.append("- $visibleText\n")
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                traverseAndExtractText(child, sb, seenText)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("ZoyaAccessibility", "Accessibility Service Connected")
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. Notification Interceptor
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val parcelableData = event.parcelableData
            if (parcelableData is android.app.Notification) {
                val extras = parcelableData.extras
                val title = extras?.getString(android.app.Notification.EXTRA_TITLE) ?: ""
                val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                val appPackage = event.packageName?.toString() ?: ""
                if (text.isNotEmpty() && !appPackage.contains("com.example") && !appPackage.contains("android")) {
                    lastNotificationSender = title
                    lastNotificationText = text
                    lastNotificationPackage = appPackage
                    lastNotificationTime = System.currentTimeMillis()
                    Log.d("ZoyaAccessibility", "Notification received: $title - $text from $appPackage")
                }
            }
            return
        }

        if (!shouldAutoClick) return
        
        val packageName = event.packageName?.toString() ?: ""
        if (packageName.contains("whatsapp")) {
            val rootNode = rootInActiveWindow ?: return
            
            // Standard click fallback when automation is idle but we want to auto-click Send
            if (targetContactToMessage == null && messageToBhej == null) {
                val clicked = searchAndClickSendButton(rootNode)
                if (clicked) {
                    Log.d("ZoyaAccessibility", "Successfully clicked send button!")
                    shouldAutoClick = false
                }
                return
            }

            // WhatsApp Search & Messaging/Calling Automation Step-by-Step Flow
            val contact = targetContactToMessage
            val mode = whatsappMode
            if (contact != null && mode != null) {
                when (automationStep) {
                    1 -> {
                        // Locate the Search bar/input or the Search action button
                        val searchEdit = findNodeByViewIdOrClass(rootNode, "com.whatsapp:id/search_src_text", "android.widget.EditText")
                        if (searchEdit != null) {
                            val arguments = android.os.Bundle()
                            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
                            searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                            Log.d("ZoyaAccessibility", "Successfully typed contact name: $contact")
                            automationStep = 2
                        } else {
                            val searchButton = findSearchButton(rootNode)
                            if (searchButton != null) {
                                performClick(searchButton)
                                Log.d("ZoyaAccessibility", "Clicked Search button icon")
                            } else {
                                val searchByDesc = findNodeByDescription(rootNode, "Search")
                                if (searchByDesc != null) {
                                    performClick(searchByDesc)
                                    Log.d("ZoyaAccessibility", "Clicked Search by description")
                                }
                            }
                        }
                    }
                    2 -> {
                        // Find the contact matching user text and click it to open chat
                        val contactNode = findNodeByText(rootNode, contact)
                        if (contactNode != null) {
                            performClick(contactNode)
                            Log.d("ZoyaAccessibility", "Clicked matching contact item: $contact")
                            automationStep = 3
                        }
                    }
                    3 -> {
                        // Type message or place call inside chat screen
                        if (mode == "message" && messageToBhej != null) {
                            val messageInput = findNodeByViewIdOrClass(rootNode, "com.whatsapp:id/entry", "android.widget.EditText")
                            if (messageInput != null) {
                                val arguments = android.os.Bundle()
                                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, messageToBhej)
                                messageInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                                Log.d("ZoyaAccessibility", "Typed message text: $messageToBhej")
                                
                                // Set up send auto click
                                targetContactToMessage = null
                                messageToBhej = null
                                whatsappMode = null
                                automationStep = 0
                                shouldAutoClick = true // clicks Send in subsequent updates
                            }
                        } else if (mode == "call") {
                            val callButton = findNodeByViewIdOrDescription(rootNode, "com.whatsapp:id/voice_call", "Voice call")
                            if (callButton != null) {
                                performClick(callButton)
                                Log.d("ZoyaAccessibility", "Triggered call sequence")
                                targetContactToMessage = null
                                messageToBhej = null
                                whatsappMode = null
                                automationStep = 0
                            }
                        }
                    }
                }
            }
        }
    }

    private fun findNodeByViewIdOrClass(node: AccessibilityNodeInfo?, id: String, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.viewIdResourceName == id || node.className?.toString()?.contains(className, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findNodeByViewIdOrClass(node.getChild(i), id, className)
            if (found != null) return found
        }
        return null
    }

    private fun findSearchButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val id = node.viewIdResourceName ?: ""
        if (id.contains("menuitem_search") || id.contains("search_button")) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findSearchButton(node.getChild(i))
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByDescription(node: AccessibilityNodeInfo?, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findNodeByDescription(node.getChild(i), desc)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val nodeText = node.text?.toString() ?: ""
        if (nodeText.contains(text, ignoreCase = true)) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) return current
                current = current.parent
            }
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findNodeByText(node.getChild(i), text)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByViewIdOrDescription(node: AccessibilityNodeInfo?, id: String, desc: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val nodeDesc = node.contentDescription?.toString() ?: ""
        val nodeId = node.viewIdResourceName ?: ""
        if (nodeId.contains(id) || nodeDesc.contains(desc, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findNodeByViewIdOrDescription(node.getChild(i), id, desc)
            if (found != null) return found
        }
        return null
    }

    private fun searchAndClickSendButton(node: AccessibilityNodeInfo): Boolean {
        // Attempt 1: by common View IDs
        val idsToTry = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send"
        )
        for (id in idsToTry) {
            val sendButtons = node.findAccessibilityNodeInfosByViewId(id)
            if (sendButtons.isNotEmpty()) {
                for (button in sendButtons) {
                    if (performClick(button)) {
                        Log.d("ZoyaAccessibility", "Clicked send button by ID: $id")
                        return true
                    }
                }
            }
        }

        // Attempt 2: Recursive search for Content Description "Send", "Bhejen", etc
        return recursiveSearchAndClick(node)
    }

    private fun recursiveSearchAndClick(node: AccessibilityNodeInfo): Boolean {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        
        if (desc == "send" || desc == "bheje" || desc == "bhejen" || desc == "envio") {
            if (performClick(node)) {
                Log.d("ZoyaAccessibility", "Clicked send button by content description!")
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (recursiveSearchAndClick(child)) {
                    return true
                }
            }
        }
        return false
    }

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        // Real visual click first
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty) {
            val x = bounds.centerX().toFloat()
            val y = bounds.centerY().toFloat()
            if (dispatchGestureClick(x, y)) {
                return true
            }
        }

        if (node.isClickable) {
            val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) return true
        }
        // Try parent if not clickable
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                val success = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (success) return true
            }
            parent = parent.parent
        }
        return false
    }

    override fun onInterrupt() {
        Log.d("ZoyaAccessibility", "Accessibility Service Interrupted")
    }
}
