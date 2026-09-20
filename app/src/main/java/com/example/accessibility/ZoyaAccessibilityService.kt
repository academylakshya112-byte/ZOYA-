package com.example.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ZoyaAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ZoyaAccessibility"
        private val mainHandler = Handler(Looper.getMainLooper())

        var shouldAutoClick = false
            set(value) {
                field = value
                if (value) {
                    startPeriodicCheck()
                    mainHandler.postDelayed({
                        field = false
                        stopPeriodicCheck()
                    }, 15000) // 15 seconds max timeout
                } else {
                    stopPeriodicCheck()
                }
            }

        var targetAppName: String = "whatsapp" // "whatsapp", "messaging", "youtube", "general"
        var instance: ZoyaAccessibilityService? = null

        // Notification storage
        var lastNotificationSender: String? = null
        var lastNotificationText: String? = null
        var lastNotificationPackage: String? = null
        var lastNotificationTime: Long = 0L

        // WhatsApp automation state
        var targetContactToMessage: String? = null
        var messageToBhej: String? = null
        var whatsappMode: String? = null // "message", "call", "open"
        var automationStep: Int = 0 // 0: Idle, 1: Search contact / find chat, 2: Select contact, 3: Type & send

        // SMS automation state
        var smsTargetNumber: String? = null
        var smsMessageText: String? = null

        // YouTube automation state
        var targetYouTubeQuery: String? = null
        var youtubeAutomationStep: Int = 0 // 0: Idle, 1: Search button find & click, 2: Typing query, 3: Clicking first video result

        private var checkRunnable: Runnable? = null

        private fun startPeriodicCheck() {
            stopPeriodicCheck()
            var attempts = 0
            checkRunnable = object : Runnable {
                override fun run() {
                    if (!shouldAutoClick || instance == null || attempts >= 40) {
                        stopPeriodicCheck()
                        return
                    }
                    attempts++
                    instance?.processActiveWindow()
                    mainHandler.postDelayed(this, 350)
                }
            }
            mainHandler.post(checkRunnable!!)
        }

        private fun stopPeriodicCheck() {
            checkRunnable?.let { mainHandler.removeCallbacks(it) }
            checkRunnable = null
        }

        fun startWhatsAppSendAutoClick(message: String?) {
            targetAppName = "whatsapp"
            messageToBhej = message
            automationStep = 3
            shouldAutoClick = true
            Log.d(TAG, "Triggered WhatsApp send auto-click mode for message: $message")
        }

        fun startSmsSendAutoClick(message: String?) {
            targetAppName = "messaging"
            smsMessageText = message
            shouldAutoClick = true
            Log.d(TAG, "Triggered SMS send auto-click mode for message: $message")
        }

        fun startWhatsAppAutomation(contactName: String, message: String?, mode: String): String {
            val inst = instance ?: return "Error: Zoya Automation Accessibility Service is not running. Please enable Zoya in Settings > Accessibility."
            targetAppName = "whatsapp"
            targetContactToMessage = contactName.trim()
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

        fun startYouTubePlayAutomation(query: String, appContext: android.content.Context): String {
            val inst = instance
            targetAppName = "youtube"
            targetYouTubeQuery = query
            youtubeAutomationStep = 1
            shouldAutoClick = true

            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", query)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                (inst ?: appContext).startActivity(intent)
                return "Opening YouTube and playing: $query"
            } catch (e: Exception) {
                try {
                    val pm = (inst ?: appContext).packageManager
                    val launchIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                    if (launchIntent != null) {
                        launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        (inst ?: appContext).startActivity(launchIntent)
                        return "Opening YouTube app to search and play: $query"
                    }
                } catch (e2: Exception) {}
                return "YouTube app is not installed on this device."
            }
        }

        fun dispatchGestureClick(x: Float, y: Float): Boolean {
            val inst = instance ?: return false
            val path = Path()
            path.moveTo(x, y)
            path.lineTo(x, y)

            val builder = GestureDescription.Builder()
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            val gesture = builder.build()

            return inst.dispatchGesture(gesture, null, null)
        }

        fun clickTextOnScreen(text: String): Boolean {
            val inst = instance ?: return false
            val root = inst.rootInActiveWindow ?: return false
            val cleanTarget = text.trim().lowercase()
            if (cleanTarget.isEmpty()) return false

            val directNodes = root.findAccessibilityNodeInfosByText(text)
            for (node in directNodes) {
                if (tryClickNodeOrGesture(node)) return true
            }

            if (deepFindAndClick(root, cleanTarget)) {
                return true
            }

            return false
        }

        private fun deepFindAndClick(node: AccessibilityNodeInfo?, target: String): Boolean {
            if (node == null) return false

            val nodeText = node.text?.toString()?.trim()?.lowercase() ?: ""
            val nodeDesc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val viewId = node.viewIdResourceName?.lowercase() ?: ""

            val matches = nodeText == target || nodeDesc == target ||
                    (target.length >= 3 && (nodeText.contains(target) || nodeDesc.contains(target) || viewId.contains(target)))

            if (matches) {
                if (tryClickNodeOrGesture(node)) return true
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (deepFindAndClick(child, target)) return true
            }

            return false
        }

        private fun tryClickNodeOrGesture(node: AccessibilityNodeInfo): Boolean {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                val bounds = Rect()
                current.getBoundsInScreen(bounds)
                if (!bounds.isEmpty) {
                    if (current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                    val x = bounds.centerX().toFloat()
                    val y = bounds.centerY().toFloat()
                    if (dispatchGestureClick(x, y)) {
                        return true
                    }
                }
                current = current.parent
            }
            return false
        }

        fun tapCoordinates(xPercent: Float, yPercent: Float): Boolean {
            val inst = instance ?: return false
            val dm = inst.resources.displayMetrics
            val clampedX = (xPercent.coerceIn(0f, 100f) / 100f) * dm.widthPixels
            val clampedY = (yPercent.coerceIn(0f, 100f) / 100f) * dm.heightPixels
            return dispatchGestureClick(clampedX, clampedY)
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
            val dm = inst.resources.displayMetrics
            val width = dm.widthPixels.toFloat()
            val height = dm.heightPixels.toFloat()

            val startX: Float
            val startY: Float
            val endX: Float
            val endY: Float

            when (direction.lowercase()) {
                "down" -> {
                    startX = width / 2f
                    startY = height * 0.8f
                    endX = width / 2f
                    endY = height * 0.2f
                }
                "up" -> {
                    startX = width / 2f
                    startY = height * 0.2f
                    endX = width / 2f
                    endY = height * 0.8f
                }
                "left" -> {
                    startX = width * 0.2f
                    startY = height / 2f
                    endX = width * 0.8f
                    endY = height / 2f
                }
                "right" -> {
                    startX = width * 0.8f
                    startY = height / 2f
                    endX = width * 0.2f
                    endY = height / 2f
                }
                else -> return false
            }

            val path = Path()
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)

            val builder = GestureDescription.Builder()
            builder.addStroke(GestureDescription.StrokeDescription(path, 0, 300))
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
        Log.d(TAG, "Accessibility Service Connected")
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
                    Log.d(TAG, "Notification received: $title - $text from $appPackage")
                }
            }
            return
        }

        if (!shouldAutoClick) return
        processActiveWindow()
    }

    fun processActiveWindow() {
        if (!shouldAutoClick) return
        val rootNode = rootInActiveWindow ?: return
        val packageName = rootNode.packageName?.toString() ?: ""

        // ─────────────────────────────────────────────
        // 1. WHATSAPP / WHATSAPP BUSINESS AUTOMATION
        // ─────────────────────────────────────────────
        if (packageName.contains("whatsapp", ignoreCase = true)) {
            handleWhatsAppAutomation(rootNode)
            return
        }

        // ─────────────────────────────────────────────
        // 2. SMS / GOOGLE MESSAGES / SAMSUNG MESSAGING AUTOMATION
        // ─────────────────────────────────────────────
        if (packageName.contains("messaging", ignoreCase = true) ||
            packageName.contains("mms", ignoreCase = true) ||
            packageName.contains("sms", ignoreCase = true) ||
            targetAppName == "messaging") {
            handleSmsAutomation(rootNode)
            return
        }

        // ─────────────────────────────────────────────
        // 3. YOUTUBE AUTOMATION
        // ─────────────────────────────────────────────
        if (packageName.contains("youtube", ignoreCase = true)) {
            handleYouTubeAutomation(rootNode)
            return
        }
    }

    private fun handleWhatsAppAutomation(rootNode: AccessibilityNodeInfo) {
        val contact = targetContactToMessage
        val message = messageToBhej
        val mode = whatsappMode

        // 1. If in Chat screen (or text already entered/loaded) -> Click Send
        if (searchAndClickWhatsAppSendButton(rootNode)) {
            Log.d(TAG, "WhatsApp: Successfully clicked send button!")
            targetContactToMessage = null
            messageToBhej = null
            whatsappMode = null
            automationStep = 0
            shouldAutoClick = false
            return
        }

        // 2. If message text is present and there is an entry box, type it and try click
        if (message != null) {
            val messageInput = findNodeByViewIdOrClass(rootNode, "com.whatsapp:id/entry", "android.widget.EditText")
                ?: findNodeByViewIdOrClass(rootNode, "com.whatsapp.w4b:id/entry", "android.widget.EditText")
            if (messageInput != null) {
                val currentText = messageInput.text?.toString() ?: ""
                if (currentText.isBlank()) {
                    val arguments = Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                    messageInput.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    Log.d(TAG, "WhatsApp: Typed message text: $message")
                }
                // Try clicking send after typing
                if (searchAndClickWhatsAppSendButton(rootNode)) {
                    targetContactToMessage = null
                    messageToBhej = null
                    whatsappMode = null
                    automationStep = 0
                    shouldAutoClick = false
                    return
                }
            }
        }

        // 3. Step 1: Search or find contact on Home/Chats list (e.g. Screenshot 1 where Kamlesh Sir is visible)
        if (contact != null) {
            when (automationStep) {
                1 -> {
                    // Check if contact name is already visible in the chats list on screen
                    val visibleContact = findContactNodeInList(rootNode, contact)
                    if (visibleContact != null) {
                        performClick(visibleContact)
                        Log.d(TAG, "WhatsApp: Clicked visible contact directly: $contact")
                        automationStep = 3
                        return
                    }

                    // Otherwise, locate search box or search icon
                    val searchEdit = findNodeByViewIdOrClass(rootNode, "com.whatsapp:id/search_src_text", "android.widget.EditText")
                        ?: findNodeByViewIdOrClass(rootNode, "com.whatsapp:id/search_input", "android.widget.EditText")
                    if (searchEdit != null) {
                        val arguments = Bundle()
                        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, contact)
                        searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        Log.d(TAG, "WhatsApp: Typed contact in search bar: $contact")
                        automationStep = 2
                    } else {
                        // Click Search icon or Ask Meta AI search holder
                        val searchButton = findNodeByViewIdOrDescription(rootNode, "com.whatsapp:id/menuitem_search", "Search")
                            ?: findNodeByViewIdOrDescription(rootNode, "com.whatsapp:id/search_holder", "Search")
                            ?: findNodeByDescription(rootNode, "Ask Meta AI or Search")
                            ?: findSearchButton(rootNode)
                        if (searchButton != null) {
                            performClick(searchButton)
                            Log.d(TAG, "WhatsApp: Clicked Search button")
                            automationStep = 2
                        }
                    }
                }
                2 -> {
                    val matchingContact = findContactNodeInList(rootNode, contact)
                    if (matchingContact != null) {
                        performClick(matchingContact)
                        Log.d(TAG, "WhatsApp: Clicked searched contact: $contact")
                        automationStep = 3
                    }
                }
                3 -> {
                    if (mode == "call") {
                        val callButton = findNodeByViewIdOrDescription(rootNode, "com.whatsapp:id/voice_call", "Voice call")
                            ?: findNodeByDescription(rootNode, "Voice call")
                            ?: findNodeByDescription(rootNode, "Call")
                        if (callButton != null) {
                            performClick(callButton)
                            Log.d(TAG, "WhatsApp: Clicked voice call button")
                            targetContactToMessage = null
                            messageToBhej = null
                            whatsappMode = null
                            automationStep = 0
                            shouldAutoClick = false
                        }
                    }
                }
            }
        }
    }

    private fun handleSmsAutomation(rootNode: AccessibilityNodeInfo) {
        val message = smsMessageText

        // 1. If message text is provided and compose box is empty, fill it
        if (!message.isNullOrBlank()) {
            val composeBox = findNodeByViewIdOrClass(rootNode, "com.google.android.apps.messaging:id/compose_message_text", "android.widget.EditText")
                ?: findNodeByClass(rootNode, "android.widget.EditText")
            if (composeBox != null) {
                val current = composeBox.text?.toString() ?: ""
                if (current.isBlank()) {
                    val args = Bundle()
                    args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, message)
                    composeBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    Log.d(TAG, "SMS: Set text in compose box: $message")
                }
            }
        }

        // 2. Click the SMS Send Button (Google Messages / Samsung Messages / Xiaomi)
        if (searchAndClickSmsSendButton(rootNode)) {
            Log.d(TAG, "SMS: Successfully clicked SMS send button!")
            smsMessageText = null
            smsTargetNumber = null
            shouldAutoClick = false
            return
        }
    }

    private fun searchAndClickWhatsAppSendButton(node: AccessibilityNodeInfo): Boolean {
        // IDs for WhatsApp send button
        val ids = listOf(
            "com.whatsapp:id/send",
            "com.whatsapp.w4b:id/send",
            "com.whatsapp:id/send_button",
            "com.whatsapp:id/send_message_button",
            "com.whatsapp:id/entry_send_btn",
            "com.whatsapp:id/input_send_button"
        )
        for (id in ids) {
            val buttons = node.findAccessibilityNodeInfosByViewId(id)
            for (btn in buttons) {
                if (performClick(btn)) return true
            }
        }

        // Check Content Descriptions
        if (searchAndClickByDescriptions(node, listOf("send", "bheje", "bhejen", "enviar", "envoyer", "kirim", "pesan", "send message"))) {
            return true
        }

        // Coordinate fallback if entry box is visible with text
        val entryBox = findNodeByViewIdOrClass(node, "com.whatsapp:id/entry", "android.widget.EditText")
        if (entryBox != null && !entryBox.text.isNullOrBlank()) {
            val dm = resources.displayMetrics
            // WhatsApp send button is located at bottom-right (~92% width, ~95% height)
            val sendX = dm.widthPixels * 0.92f
            val sendY = dm.heightPixels * 0.95f
            if (dispatchGestureClick(sendX, sendY)) {
                Log.d(TAG, "WhatsApp: Dispatched gesture tap on bottom-right send button")
                return true
            }
        }

        return false
    }

    private fun searchAndClickSmsSendButton(node: AccessibilityNodeInfo): Boolean {
        // Known SMS app Send button View IDs (Google Messages, Samsung, AOSP, MIUI)
        val ids = listOf(
            "com.google.android.apps.messaging:id/send_message_button_icon",
            "com.google.android.apps.messaging:id/send_message_button",
            "com.google.android.apps.messaging:id/compose_send_button",
            "com.google.android.apps.messaging:id/send_button",
            "com.samsung.android.messaging:id/send_button",
            "com.android.mms:id/send_button",
            "send_message_button",
            "send_button"
        )
        for (id in ids) {
            val buttons = node.findAccessibilityNodeInfosByViewId(id)
            for (btn in buttons) {
                if (performClick(btn)) return true
            }
        }

        // Content Descriptions for SMS Send Button
        val descriptions = listOf(
            "send sms", "send", "send message", "send rcs message",
            "send sms to", "sim 1 send", "sim 2 send", "bhejen", "bheje", "send text"
        )
        if (searchAndClickByDescriptions(node, descriptions)) {
            return true
        }

        // Fallback for SMS apps: Click bottom-right green/blue circular button (as in user's screenshot 2)
        val composeBox = findNodeByClass(node, "android.widget.EditText")
        if (composeBox != null && !composeBox.text.isNullOrBlank()) {
            val dm = resources.displayMetrics
            val sendX = dm.widthPixels * 0.92f
            val sendY = dm.heightPixels * 0.94f
            if (dispatchGestureClick(sendX, sendY)) {
                Log.d(TAG, "SMS: Dispatched gesture tap on bottom-right SMS send button")
                return true
            }
        }

        return false
    }

    private fun findContactNodeInList(root: AccessibilityNodeInfo, targetName: String): AccessibilityNodeInfo? {
        val cleanTarget = targetName.trim().lowercase()
        val directNodes = root.findAccessibilityNodeInfosByText(targetName)
        for (node in directNodes) {
            val nodeText = node.text?.toString()?.trim()?.lowercase() ?: ""
            if (nodeText.contains(cleanTarget) || cleanTarget.contains(nodeText)) {
                var clickTarget: AccessibilityNodeInfo? = node
                while (clickTarget != null) {
                    if (clickTarget.isClickable) return clickTarget
                    clickTarget = clickTarget.parent
                }
                return node
            }
        }

        return deepFindContactNode(root, cleanTarget)
    }

    private fun deepFindContactNode(node: AccessibilityNodeInfo?, target: String): AccessibilityNodeInfo? {
        if (node == null) return null
        val text = node.text?.toString()?.trim()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""

        val firstWord = target.split(" ").firstOrNull() ?: target
        if (text.contains(target) || desc.contains(target) || (firstWord.length > 2 && text.contains(firstWord))) {
            var current: AccessibilityNodeInfo? = node
            while (current != null) {
                if (current.isClickable) return current
                current = current.parent
            }
            return node
        }

        for (i in 0 until node.childCount) {
            val found = deepFindContactNode(node.getChild(i), target)
            if (found != null) return found
        }
        return null
    }

    private fun handleYouTubeAutomation(rootNode: AccessibilityNodeInfo) {
        val query = targetYouTubeQuery ?: return
        when (youtubeAutomationStep) {
            1 -> {
                val searchEdit = findNodeByViewIdOrClass(rootNode, "com.google.android.youtube:id/search_edit_text", "android.widget.EditText")
                if (searchEdit != null) {
                    val arguments = Bundle()
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
                    searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    Log.d(TAG, "YouTube: Typed query '$query'")
                    youtubeAutomationStep = 2

                    mainHandler.postDelayed({
                        rootInActiveWindow?.let { currentRoot ->
                            if (clickFirstVideoResult(currentRoot)) {
                                targetYouTubeQuery = null
                                youtubeAutomationStep = 0
                                shouldAutoClick = false
                            }
                        }
                    }, 1000)
                } else {
                    val searchButton = findNodeByViewIdOrDescription(rootNode, "com.google.android.youtube:id/menu_item_search", "Search")
                        ?: findNodeByDescription(rootNode, "Search YouTube")
                        ?: findSearchButton(rootNode)
                    if (searchButton != null) {
                        performClick(searchButton)
                        Log.d(TAG, "YouTube: Clicked search button")
                        youtubeAutomationStep = 2
                    } else if (clickFirstVideoResult(rootNode)) {
                        targetYouTubeQuery = null
                        youtubeAutomationStep = 0
                        shouldAutoClick = false
                    }
                }
            }
            2, 3 -> {
                if (clickFirstVideoResult(rootNode)) {
                    Log.d(TAG, "YouTube: Played top video for query '$query'")
                    targetYouTubeQuery = null
                    youtubeAutomationStep = 0
                    shouldAutoClick = false
                }
            }
        }
    }

    private fun clickFirstVideoResult(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        val thumbnails = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/thumbnail")
        for (thumb in thumbnails) {
            if (performClick(thumb)) return true
        }

        val resultsNodes = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/results")
        for (node in resultsNodes) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null && (child.isClickable || child.childCount > 0)) {
                    if (performClick(child)) return true
                }
            }
        }

        return findAndClickFirstVideoCard(rootNode)
    }

    private fun findAndClickFirstVideoCard(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        if ((desc.contains("views") || desc.contains("minute") || desc.contains("seconds") || desc.contains("hour") || desc.contains("play video") || desc.contains("official video") || desc.contains("song") || desc.contains("music")) && (node.isClickable || node.childCount > 0)) {
            if (performClick(node)) return true
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (findAndClickFirstVideoCard(child)) return true
        }
        return false
    }

    private fun searchAndClickByDescriptions(node: AccessibilityNodeInfo, descs: List<String>): Boolean {
        val nodeDesc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
        for (d in descs) {
            if (nodeDesc == d || nodeDesc.startsWith(d)) {
                if (performClick(node)) return true
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null && searchAndClickByDescriptions(child, descs)) {
                return true
            }
        }
        return false
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

    private fun findNodeByClass(node: AccessibilityNodeInfo?, className: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.className?.toString()?.contains(className, ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val found = findNodeByClass(node.getChild(i), className)
            if (found != null) return found
        }
        return null
    }

    private fun findSearchButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        val id = node.viewIdResourceName ?: ""
        if (id.contains("menuitem_search") || id.contains("search_button") || id.contains("search_holder")) {
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

    private fun performClick(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
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
        Log.d(TAG, "Accessibility Service Interrupted")
        stopPeriodicCheck()
    }
}
