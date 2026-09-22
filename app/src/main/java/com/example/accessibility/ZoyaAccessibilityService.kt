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
            val cleanQuery = if (query.isBlank() ||
                query.contains("hindi song", ignoreCase = true) ||
                query.contains("achha song", ignoreCase = true) ||
                query.contains("badhiya song", ignoreCase = true) ||
                query.equals("song", ignoreCase = true) ||
                query.equals("gana", ignoreCase = true) ||
                query.equals("gaana", ignoreCase = true) ||
                query.equals("music", ignoreCase = true)) {
                "Trending Hit Hindi Songs"
            } else {
                query.trim()
            }

            val inst = instance
            targetAppName = "youtube"
            targetYouTubeQuery = cleanQuery
            youtubeAutomationStep = 1 // Step 1: Open YouTube & Tap Search
            shouldAutoClick = true

            // Schedule proactive delayed fallback checks
            mainHandler.postDelayed({
                if (shouldAutoClick && targetAppName == "youtube") {
                    instance?.processActiveWindow()
                }
            }, 800)

            mainHandler.postDelayed({
                if (shouldAutoClick && targetAppName == "youtube") {
                    instance?.processActiveWindow()
                }
            }, 1800)

            mainHandler.postDelayed({
                if (shouldAutoClick && targetAppName == "youtube") {
                    instance?.processActiveWindow()
                }
            }, 3200)

            // Step 1: Launch YouTube main launch intent so user sees YouTube opening visibly
            try {
                val pm = (inst ?: appContext).packageManager
                val launchIntent = pm.getLaunchIntentForPackage("com.google.android.youtube")
                if (launchIntent != null) {
                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    (inst ?: appContext).startActivity(launchIntent)
                    return "YouTube open karke '$cleanQuery' search karke play kar rahi hoon..."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error launching YouTube package intent", e)
            }

            // Fallback: ACTION_SEARCH intent
            try {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", cleanQuery)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                (inst ?: appContext).startActivity(intent)
                return "YouTube par '$cleanQuery' search kar rahi hoon..."
            } catch (e2: Exception) {
                return "YouTube app is not installed on this device."
            }
        }

        fun pressBack(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        }

        fun pressHome(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        }

        fun pressRecents(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
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
                val title = extras?.getString(android.app.Notification.EXTRA_TITLE) 
                    ?: extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() 
                    ?: ""
                val text = extras?.getString(android.app.Notification.EXTRA_TEXT) 
                    ?: extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() 
                    ?: ""
                val appPackage = event.packageName?.toString() ?: ""
                if ((text.isNotEmpty() || title.isNotEmpty()) && !appPackage.contains("com.example") && appPackage != "android") {
                    lastNotificationSender = title
                    lastNotificationText = text
                    lastNotificationPackage = appPackage
                    lastNotificationTime = System.currentTimeMillis()
                    Log.d(TAG, "Notification received via Accessibility: $title - $text from $appPackage")
                    try {
                        com.example.notification.NotificationManagerHelper.processRawNotification(this, appPackage, title, text)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing accessibility notification", e)
                    }
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

        // 1. If video is already actively playing, finish automation cleanly
        if (isYouTubeVideoActivelyPlaying(rootNode)) {
            Log.d(TAG, "YouTube: Video playback confirmed active! Finishing automation.")
            targetYouTubeQuery = null
            youtubeAutomationStep = 0
            shouldAutoClick = false
            return
        }

        // 2. Check if Search Results are already visible on screen (as shown in user's screenshot)
        if (isYouTubeSearchResults(rootNode)) {
            if (clickFirstPlayableSongResult(rootNode)) {
                Log.d(TAG, "YouTube: Successfully clicked playable song result for query '$query'")
                // Wait briefly for playback to engage before releasing
                mainHandler.postDelayed({
                    rootInActiveWindow?.let { currentRoot ->
                        if (isYouTubeVideoActivelyPlaying(currentRoot)) {
                            targetYouTubeQuery = null
                            youtubeAutomationStep = 0
                            shouldAutoClick = false
                        }
                    }
                }, 1500)
                return
            }
        }

        // 3. Check if search edit text is present on screen (user sees search bar)
        val searchEdit = findNodeByViewIdOrClass(rootNode, "com.google.android.youtube:id/search_edit_text", "android.widget.EditText")
        if (searchEdit != null) {
            val currentText = searchEdit.text?.toString() ?: ""
            if (!currentText.equals(query, ignoreCase = true)) {
                // Visibly type query into search edit text
                val arguments = Bundle()
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query)
                searchEdit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                Log.d(TAG, "YouTube: Visibly typed query '$query' into search bar")
                youtubeAutomationStep = 2
            }

            // Submit the search query visibly
            mainHandler.postDelayed({
                rootInActiveWindow?.let { currentRoot ->
                    val currentEdit = findNodeByViewIdOrClass(currentRoot, "com.google.android.youtube:id/search_edit_text", "android.widget.EditText")
                    if (currentEdit != null) {
                        submitYouTubeSearch(currentRoot, currentEdit)
                    }
                }
            }, 350)
            return
        }

        // 4. If on YouTube home screen without search open, click search icon once
        val searchButton = findYouTubeSearchButton(rootNode)
        if (searchButton != null) {
            performClick(searchButton)
            Log.d(TAG, "YouTube: Clicked Search icon on Home screen to open search")
            youtubeAutomationStep = 2
            return
        }
    }

    private fun isYouTubeSearchResults(rootNode: AccessibilityNodeInfo): Boolean {
        // Query text in header or search clear button
        val searchEdit = findNodeByViewIdOrClass(rootNode, "com.google.android.youtube:id/search_edit_text", "android.widget.EditText")
        val hasText = searchEdit?.text?.toString()?.isNotBlank() == true

        val hasAd = rootNode.findAccessibilityNodeInfosByText("Sponsored").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Install").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Spotify").isNotEmpty()
        val hasMixOrChannel = rootNode.findAccessibilityNodeInfosByText("Mix").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Curated by YouTube").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Subscribe").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("View Channel").isNotEmpty()
        val hasChips = rootNode.findAccessibilityNodeInfosByText("All").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Shorts").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Videos").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Filter").isNotEmpty()

        return hasText || (hasAd && hasMixOrChannel) || (hasMixOrChannel && hasChips)
    }

    private fun findYouTubeSearchButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val dm = resources.displayMetrics
        val searchButton = findNodeByViewIdOrDescription(rootNode, "com.google.android.youtube:id/menu_item_search", "Search")
            ?: findNodeByDescription(rootNode, "Search YouTube")
            ?: findNodeByDescription(rootNode, "Search videos")
            ?: findNodeByDescription(rootNode, "खोजें")
            ?: findSearchButton(rootNode)

        if (searchButton != null) return searchButton

        // Check top right header bar for clickable icon
        val b = Rect()
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            child.getBoundsInScreen(b)
            if (b.top < dm.heightPixels * 0.12f && b.right > dm.widthPixels * 0.70f && child.isClickable) {
                return child
            }
        }
        return null
    }

    private fun submitYouTubeSearch(rootNode: AccessibilityNodeInfo, searchEdit: AccessibilityNodeInfo) {
        val query = targetYouTubeQuery ?: "Trending Hit Hindi Songs"
        val dm = resources.displayMetrics

        // 1. Look for first search suggestion item in drop-down
        val suggestions = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/search_suggestion")
        if (suggestions.isNotEmpty()) {
            val first = suggestions.first()
            if (performClick(first)) {
                Log.d(TAG, "YouTube: Clicked first search suggestion row")
                return
            }
        }

        // 2. Perform IME search / click on searchEdit
        searchEdit.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // 3. Dispatch gesture tap on keyboard bottom-right Enter/Search key
        val enterX = dm.widthPixels * 0.90f
        val enterY = dm.heightPixels * 0.94f
        dispatchGestureClick(enterX, enterY)
        Log.d(TAG, "YouTube: Dispatched keyboard search tap at ($enterX, $enterY)")

        // 4. Proactive safety fallback: Fire ACTION_SEARCH intent after 600ms if results don't appear
        mainHandler.postDelayed({
            if (shouldAutoClick && targetAppName == "youtube") {
                rootInActiveWindow?.let { currentRoot ->
                    if (!isYouTubeSearchResults(currentRoot)) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEARCH).apply {
                                setPackage("com.google.android.youtube")
                                putExtra("query", query)
                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {}
                    }
                }
            }
        }, 600)
    }

    private fun clickFirstPlayableSongResult(rootNode: AccessibilityNodeInfo): Boolean {
        val dm = resources.displayMetrics
        val screenHeight = dm.heightPixels
        val screenWidth = dm.widthPixels

        // 1. Measure bottom boundary of any Sponsored Ads (e.g. Spotify Sponsored ad in screenshot)
        var adBottom = 0
        val adNodes = mutableListOf<AccessibilityNodeInfo>()
        collectAdNodes(rootNode, adNodes)
        for (ad in adNodes) {
            val b = Rect()
            ad.getBoundsInScreen(b)
            if (b.bottom > adBottom && b.bottom < screenHeight * 0.50f) {
                adBottom = b.bottom
            }
        }

        // 2. Measure bottom boundary of Channel Topic banner (e.g. "Trending - Topic" with Subscribe/View Channel in screenshot)
        var channelBottom = 0
        val channelNodes = mutableListOf<AccessibilityNodeInfo>()
        collectChannelBannerNodes(rootNode, channelNodes)
        for (ch in channelNodes) {
            val b = Rect()
            ch.getBoundsInScreen(b)
            if (b.bottom > channelBottom && b.bottom < screenHeight * 0.65f) {
                channelBottom = b.bottom
            }
        }

        // Playable content starts strictly below Ads and Channel banners
        val safeTopCutoff = maxOf(screenHeight * 0.12f, adBottom.toFloat(), channelBottom.toFloat())
        Log.d(TAG, "YouTube Search Results: adBottom=$adBottom, channelBottom=$channelBottom, safeTopCutoff=$safeTopCutoff")

        // 3. Search accessibility tree for genuine playable video / mix / playlist item
        val playableCandidate = findPlayableSongNode(rootNode, safeTopCutoff.toInt(), screenHeight)
        if (playableCandidate != null) {
            val bounds = Rect()
            playableCandidate.getBoundsInScreen(bounds)

            // If the item is partially cut off at screen bottom, scroll down to reveal it properly
            if (bounds.bottom > screenHeight * 0.90f && bounds.top > safeTopCutoff) {
                Log.d(TAG, "YouTube: Playable item partially off screen at bottom, scrolling down...")
                scrollScreen("down")
                return false
            }

            val tapX = bounds.centerX().toFloat().coerceIn(screenWidth * 0.15f, screenWidth * 0.85f)
            val tapY = bounds.centerY().toFloat()

            var clicked = performClick(playableCandidate)
            if (dispatchGestureClick(tapX, tapY)) {
                clicked = true
            }

            if (clicked) {
                Log.d(TAG, "YouTube: Successfully clicked song/mix item at ($tapX, $tapY)")
                return true
            }
        }

        // 4. Fallback coordinate tap right on the first video/mix card (skipping ads and channels)
        val tapX = screenWidth * 0.50f
        val playableAreaHeight = (screenHeight * 0.86f) - safeTopCutoff
        val tapY = if (playableAreaHeight > 150) {
            safeTopCutoff + (playableAreaHeight * 0.38f)
        } else {
            screenHeight * 0.62f
        }

        Log.d(TAG, "YouTube: Fallback gesture tap on song/mix result at ($tapX, $tapY)")
        return dispatchGestureClick(tapX, tapY)
    }

    private fun findPlayableSongNode(node: AccessibilityNodeInfo?, minTop: Int, screenHeight: Int): AccessibilityNodeInfo? {
        if (node == null) return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Ignore nodes above our safe cutoff
        if (bounds.bottom <= minTop) return null

        // Ignore ads and channel banners directly
        if (isAdOrPromoNode(node) || isChannelBannerNode(node)) return null

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""

        val isPlayableIndicator = desc.contains("mix") || text.contains("mix") ||
            desc.contains("curated by youtube") || text.contains("curated by youtube") ||
            desc.contains("playlist") || text.contains("playlist") ||
            desc.contains("views") || text.contains("views") ||
            desc.contains("minute") || desc.contains("seconds") ||
            desc.contains("play video") || text.contains("play video") ||
            desc.contains("song") || text.contains("song") ||
            Regex("\\d+:\\d+").containsMatchIn(text) || Regex("\\d+:\\d+").containsMatchIn(desc)

        val viewId = node.viewIdResourceName ?: ""
        val isThumbnail = viewId.contains("thumbnail") || viewId.contains("compact_link")

        if ((isPlayableIndicator || isThumbnail) && bounds.top >= minTop - 30 && bounds.top < screenHeight * 0.85f && bounds.width() > 100 && bounds.height() > 70) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val found = findPlayableSongNode(child, minTop, screenHeight)
            if (found != null) return found
        }
        return null
    }

    private fun collectAdNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (isAdOrPromoNode(node)) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectAdNodes(node.getChild(i), list)
        }
    }

    private fun isAdOrPromoNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val id = node.viewIdResourceName?.lowercase() ?: ""
        return text.contains("sponsored") || text.contains("install") || text == "ad" || text.startsWith("ad •") ||
            desc.contains("sponsored") || desc.contains("promoted") || id.contains("ad_") ||
            text.contains("spotify") || desc.contains("spotify")
    }

    private fun collectChannelBannerNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        if (isChannelBannerNode(node)) {
            list.add(node)
        }
        for (i in 0 until node.childCount) {
            collectChannelBannerNodes(node.getChild(i), list)
        }
    }

    private fun isChannelBannerNode(node: AccessibilityNodeInfo): Boolean {
        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        return text == "subscribe" || text == "view channel" || text.contains("subscribers") ||
            desc == "subscribe" || desc == "view channel" || desc.contains("subscribers")
    }

    private fun isYouTubeVideoActivelyPlaying(rootNode: AccessibilityNodeInfo): Boolean {
        val searchEdit = findNodeByViewIdOrClass(rootNode, "com.google.android.youtube:id/search_edit_text", "android.widget.EditText")
        if (searchEdit != null && searchEdit.isVisibleToUser) {
            return false
        }

        val hasPause = rootNode.findAccessibilityNodeInfosByText("Pause video").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Pause").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("विराम").isNotEmpty()

        val playerViews = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/player_view")
        if (playerViews.isNotEmpty()) return true

        val watchLayouts = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.youtube:id/watch_while_layout")
        val hasWatchControls = rootNode.findAccessibilityNodeInfosByText("Like this video").isNotEmpty() ||
            rootNode.findAccessibilityNodeInfosByText("Comments").isNotEmpty()

        return hasPause || (watchLayouts.isNotEmpty() && hasWatchControls)
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
