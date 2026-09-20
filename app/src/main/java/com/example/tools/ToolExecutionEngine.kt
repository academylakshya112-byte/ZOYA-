package com.example.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ToolExecutionEngine(private val context: Context) {

    suspend fun execute(name: String, args: JsonObject): String = withContext(Dispatchers.IO) {
        try {
            when (name) {
                "openApp" -> {
                    val appName = args["packageName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing packageName"
                    openAppGeneric(appName)
                }
                "searchAndCallContact" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val useDialer = args["useDialer"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                    val simSlot = args["simSlot"]?.jsonPrimitive?.content?.toIntOrNull()
                    callContact(contactName, useDialer, simSlot)
                }
                "sendWhatsAppMessage" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val message = args["message"]?.jsonPrimitive?.content ?: ""
                    sendWhatsApp(contactName, message)
                }
                "sendGmail" -> {
                    val recipient = args["recipientEmail"]?.jsonPrimitive?.content ?: ""
                    val subject = args["subject"]?.jsonPrimitive?.content ?: ""
                    val body = args["body"]?.jsonPrimitive?.content ?: ""
                    sendEmail(recipient, subject, body)
                }
                "searchYouTube" -> {
                    val query = args["query"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing query"
                    searchYouTube(query)
                }
                "playFavoriteSong" -> {
                    val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
                    val song = prefs.getString("favorite_song", "") ?: ""
                    val app = prefs.getString("preferred_music_app", "YT Music") ?: "YT Music"
                    if (song.isBlank()) {
                        "No favorite song configured in Settings yet. Please set it in Settings."
                    } else {
                        if (app.equals("Spotify", ignoreCase = true)) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$song")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            try {
                                context.startActivity(intent)
                                "Playing favorite song '$song' on Spotify."
                            } catch (e: Exception) {
                                searchYouTube(song)
                            }
                        } else {
                            searchYouTube(song)
                        }
                    }
                }
                "triggerSosEmergency" -> {
                    val list = com.example.model.SosContactsManager.getSosContacts(context)
                    if (list.isEmpty()) {
                        "No SOS contacts saved in Settings. Please add an SOS contact in Settings first."
                    } else {
                        val sos = list.first()
                        callContact(sos.phone, false, null)
                        "Initiating emergency SOS call to ${sos.name} (${sos.phone})."
                    }
                }
                "adjustVolume" -> {
                    val direction = args["direction"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing direction (up/down/mute/unmute/max)"
                    adjustSystemVolume(direction)
                }
                "toggleTorch" -> {
                    val state = args["state"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing state (on/off)"
                    toggleTorch(state)
                }
                "setBrightness" -> {
                    val levelStr = args["level"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing level"
                    val level = levelStr.toIntOrNull() ?: 50
                    setBrightness(level)
                }
                "setVolumePercent" -> {
                    val percentStr = args["percent"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing percent"
                    val percent = percentStr.toIntOrNull() ?: 50
                    setVolumePercent(percent)
                }
                "openNotificationPanel" -> {
                    openNotificationPanel()
                }
                "openQuickSettings" -> {
                    val service = com.example.accessibility.ZoyaAccessibilityService.instance
                    if (service != null && service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)) {
                        "Opened quick settings panel."
                    } else "Failed to open."
                }
                "clickTextOnScreen" -> {
                    val text = args["text"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing text"
                    val success = com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen(text)
                    if (success) "Clicked on '$text'." else "Failed to click on '$text'."
                }
                "readScreenText" -> {
                    com.example.accessibility.ZoyaAccessibilityService.getScreenText()
                }
                "readLastNotification" -> {
                    val sender = com.example.accessibility.ZoyaAccessibilityService.lastNotificationSender
                    val text = com.example.accessibility.ZoyaAccessibilityService.lastNotificationText
                    val pkg = com.example.accessibility.ZoyaAccessibilityService.lastNotificationPackage
                    if (sender != null && text != null) {
                        "Last received notification:\nFrom: $sender (App: $pkg)\nMessage: $text"
                    } else {
                        "No new notifications have been intercepted yet."
                    }
                }
                "whatsappSearchAndMessage" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val message = args["message"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing message"
                    com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(contactName, message, "message")
                }
                "whatsappSearchAndCall" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(contactName, null, "call")
                }
                "scrollScreen" -> {
                    val direction = args["direction"]?.jsonPrimitive?.content ?: "down"
                    val success = com.example.accessibility.ZoyaAccessibilityService.scrollScreen(direction)
                    if (success) "Scrolled screen $direction." else "Failed to scroll screen."
                }
                "rememberFact" -> {
                    val topic = args["topic"]?.jsonPrimitive?.content ?: "General"
                    val fact = args["fact"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing fact"
                    com.example.memory.MemoryManager.saveFact(context, topic, fact)
                }
                "teachSkill" -> {
                    val trigger = args["trigger"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing trigger"
                    val actionOrRule = args["actionOrRule"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing actionOrRule"
                    com.example.memory.MemoryManager.teachSkill(context, trigger, actionOrRule)
                }
                "getLearnedMemories" -> {
                    com.example.memory.MemoryManager.getMemoriesSummary(context)
                }
                "forgetMemory" -> {
                    val key = args["key"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing key"
                    com.example.memory.MemoryManager.forgetMemory(context, key)
                }
                "sendSmsMessage" -> {
                    val contactOrNumber = args["contactNameOrNumber"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactNameOrNumber"
                    val message = args["message"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing message"
                    sendSms(contactOrNumber, message)
                }
                "findContact" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val matches = findContacts(contactName)
                    if (matches.isEmpty()) {
                        "Contact '$contactName' nahi mila. Unka number bata do ya naam dobara bata do."
                    } else if (matches.size == 1) {
                        "Contact mil gaya: ${matches.first().first} (${matches.first().second})"
                    } else {
                        val names = matches.joinToString("\n") { "- ${it.first}: ${it.second}" }
                        "Is naam ke multiple contacts mile hain:\n$names\nKis wale ko select karna hai?"
                    }
                }
                "getContactNumbers" -> {
                    val contactName = args["contactName"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactName"
                    val matches = findContacts(contactName)
                    if (matches.isEmpty()) {
                        "Contact '$contactName' ka koi number nahi mila."
                    } else {
                        val numbers = matches.joinToString(", ") { "${it.first}: ${it.second}" }
                        "$contactName ka phone number: $numbers"
                    }
                }
                "openWhatsAppChat" -> {
                    val contactNameOrNumber = args["contactNameOrNumber"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing contactNameOrNumber"
                    openWhatsAppChat(contactNameOrNumber)
                }
                "saveContact" -> {
                    val name = args["name"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing name"
                    val number = args["number"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing number"
                    saveContact(name, number)
                }
                "setConfirmationMode" -> {
                    val enabledStr = args["enabled"]?.jsonPrimitive?.content ?: "true"
                    val enabled = enabledStr.toBooleanStrictOrNull() ?: true
                    val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("confirmation_mode", enabled).apply()
                    if (enabled) "Confirmation mode ON ho gaya hai. Ab message bhejne ya call lagane se pehle aapse pucha jayega."
                    else "Instant mode ON ho gaya hai. Ab bina confirmation ke turant execute kiya jayega."
                }
                "getRecentNotifications" -> {
                    com.example.notification.NotificationManagerHelper.getRecentSummary()
                }
                "configureNotificationSettings" -> {
                    configureNotificationSettings(args)
                }
                "getNotificationSettings" -> {
                    getNotificationSettings()
                }
                "answerIncomingCall" -> {
                    answerIncomingCall()
                }
                "rejectIncomingCall" -> {
                    rejectIncomingCall()
                }
                "getSimCardInfo" -> {
                    getSimCardInfo()
                }
                "playMedia" -> {
                    val query = args["query"]?.jsonPrimitive?.content ?: return@withContext "Error: Missing query"
                    playMedia(query)
                }
                else -> "Error: Tool $name not found."
            }
        } catch (e: Exception) {
            "Error executing $name: ${e.message}"
        }
    }

    private fun openAppGeneric(appName: String): String {
        val lowerName = appName.lowercase()

        if (lowerName == "camera" || lowerName == "kamera") {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
                return "Camera opened"
            } catch (e: Exception) {
                // Fallback to searching packages
            }
        }

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(0)
        
        var targetPackage: String? = null
        for (app in packages) {
            val name = pm.getApplicationLabel(app).toString().lowercase()
            if (name.contains(lowerName)) {
                targetPackage = app.packageName
                break
            }
        }

        if (targetPackage == null) {
            return "Could not find an installed app matching '$appName'"
        }

        val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return "App '$appName' launched successfully."
        }
        return "Could not launch app '$appName'"
    }

    private fun callContact(nameOrNumber: String, useDialer: Boolean = false, simSlot: Int? = null): String {
        if (nameOrNumber == "121" || nameOrNumber == "*121#") {
            return "ERROR: You tried to call 121 instead of using the contact name. DO NOT invent numbers. Use the contact name provided by the user (e.g. 'Rohit')."
        }

        val isNumber = nameOrNumber.count { it.isDigit() } >= 7 || nameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))
        
        val number = if (isNumber) {
            nameOrNumber.replace(Regex("[^0-9+*#]"), "")
        } else {
            val matches = findContacts(nameOrNumber)
            if (matches.isEmpty()) return "Could not find a phone number for '$nameOrNumber'. Please ask the user for the correct name."
            matches.first().second
        }

        val action = if (useDialer) Intent.ACTION_DIAL else Intent.ACTION_CALL
        val callIntent = Intent(action)
        callIntent.data = Uri.parse("tel:$number")
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        if (!useDialer && simSlot != null) {
            try {
                // Common extras for sim selection
                val slotIndex = simSlot - 1
                callIntent.putExtra("com.android.phone.force.slot", true)
                callIntent.putExtra("com.android.phone.extra.slot", slotIndex)
                callIntent.putExtra("simSlot", slotIndex) // For some samsung / older models
                
                // For modern android versions, use TelecomManager
                if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    val phoneAccounts = telecomManager.callCapablePhoneAccounts
                    if (slotIndex in 0 until phoneAccounts.size) {
                        callIntent.putExtra(android.telecom.TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccounts[slotIndex])
                    }
                }
            } catch (e: Exception) {
                // Ignore exceptions with telecom manager
            }
        }
        
        try {
            context.startActivity(callIntent)
            return if (useDialer) "Opened dialer for $nameOrNumber ($number)" else "Calling $nameOrNumber ($number) via SIM $simSlot..."
        } catch (e: SecurityException) {
            return "Missing CALL_PHONE permission."
        }
    }

    private fun normalizePhoneNumber(rawNumber: String): String {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val countryCodePref = prefs.getString("country_code", "India (+91)") ?: "India (+91)"
        val defaultCountryCode = Regex("\\+?([0-9]{1,4})").find(countryCodePref)?.groupValues?.get(1) ?: "91"
        
        val digits = rawNumber.replace(Regex("[^0-9]"), "")
        return when {
            digits.length == 10 -> defaultCountryCode + digits
            digits.length == 11 && digits.startsWith("0") -> defaultCountryCode + digits.substring(1)
            digits.length >= 11 -> digits
            else -> digits
        }
    }

    private fun sendWhatsApp(nameOrNumber: String, message: String): String {
        if (nameOrNumber == "121" || nameOrNumber == "*121#") {
            return "ERROR: You tried to use 121 instead of the contact name. DO NOT invent numbers. Use the exact contact name provided by the user."
        }

        val isNumber = nameOrNumber.count { it.isDigit() } >= 7 || nameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))
        
        if (isNumber) {
            val cleanNumber = normalizePhoneNumber(nameOrNumber)
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                com.example.accessibility.ZoyaAccessibilityService.startWhatsAppSendAutoClick(message)
                context.startActivity(intent)
                return "Opening WhatsApp and sending message to $nameOrNumber."
            } catch (e: Exception) {
                return com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(nameOrNumber, message, "message")
            }
        }

        val matches = findContacts(nameOrNumber)
        if (matches.isNotEmpty()) {
            val contactNumber = matches.first().second
            val cleanNumber = normalizePhoneNumber(contactNumber)
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                com.example.accessibility.ZoyaAccessibilityService.startWhatsAppSendAutoClick(message)
                context.startActivity(intent)
                return "Opening WhatsApp chat for ${matches.first().first} and sending message: '$message'"
            } catch (e: Exception) {
                return com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(nameOrNumber, message, "message")
            }
        } else {
            return com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(nameOrNumber, message, "message")
        }
    }

    private fun sendEmail(recipient: String, subject: String, body: String): String {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") 
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(emailIntent)
            return "Opened email client with draft."
        } catch (e: Exception) {
            return "No email client found."
        }
    }

    private fun searchYouTube(query: String): String {
        val cleanQuery = if (query.isBlank() || query.equals("song", ignoreCase = true) || query.equals("hindi song", ignoreCase = true) || query.equals("achha song", ignoreCase = true)) {
            "Trending Hindi Hit Songs 2026"
        } else {
            query
        }
        return com.example.accessibility.ZoyaAccessibilityService.startYouTubePlayAutomation(cleanQuery, context)
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    private fun findContacts(namePattern: String): List<Pair<String, String>> {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        
        val initialResults = internalFindContacts(namePattern)
        if (initialResults.isNotEmpty()) return initialResults

        // If no match found, strip common titles/honorifics and search again
        val normalized = namePattern.lowercase()
            .replace(Regex("\\b(sir|ji|master|madam|mam|bhai|bhaiya|didi|uncle|aunty|dr|mr|mrs|prof)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
            
        if (normalized.isNotEmpty() && normalized != namePattern.lowercase().trim()) {
            val fallbackResults = internalFindContacts(normalized)
            if (fallbackResults.isNotEmpty()) return fallbackResults
        }

        return emptyList()
    }

    private fun internalFindContacts(namePattern: String): List<Pair<String, String>> {
        try {
            val fallbackUri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val fbProjection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            context.contentResolver.query(fallbackUri, fbProjection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                val exactMatches = mutableListOf<Pair<String, String>>()
                val startsWithMatches = mutableListOf<Pair<String, String>>()
                val containsMatches = mutableListOf<Pair<String, String>>()
                val fuzzyMatches = mutableListOf<Pair<Int, Pair<String, String>>>()
                
                val cleanPattern = namePattern.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
                val searchWords = cleanPattern.split(" ").filter { it.isNotEmpty() }

                while (cursor.moveToNext()) {
                    val contactName = cursor.getString(nameIdx) ?: continue
                    val contactNum = cursor.getString(numIdx) ?: continue
                    
                    val cleanContactName = contactName.lowercase().replace(Regex("[^a-z0-9 ]"), "").trim()
                    
                    if (cleanContactName.isEmpty()) continue

                    val contactNameNoSpace = cleanContactName.replace(" ", "")
                    val patternNoSpace = cleanPattern.replace(" ", "")

                    if (contactNameNoSpace == patternNoSpace || cleanContactName == cleanPattern) {
                        exactMatches.add(Pair(contactName, contactNum))
                    } else if (contactNameNoSpace.startsWith(patternNoSpace) || cleanContactName.startsWith(cleanPattern)) {
                        startsWithMatches.add(Pair(contactName, contactNum))
                    } else if (searchWords.isNotEmpty() && searchWords.all { cleanContactName.contains(it) }) {
                        containsMatches.add(Pair(contactName, contactNum))
                    } else if (contactNameNoSpace.contains(patternNoSpace) && patternNoSpace.length > 2) {
                        containsMatches.add(Pair(contactName, contactNum))
                    }
                    
                    val distance = levenshtein(contactNameNoSpace, patternNoSpace)
                    // If within reasonable error margin (e.g. 2 typos)
                    if (distance <= 2 && patternNoSpace.length > 3) {
                        fuzzyMatches.add(Pair(distance, Pair(contactName, contactNum)))
                    }
                }
                
                if (exactMatches.isNotEmpty()) return exactMatches.distinctBy { it.second }
                if (startsWithMatches.isNotEmpty()) return startsWithMatches.distinctBy { it.second }
                if (containsMatches.isNotEmpty()) return containsMatches.distinctBy { it.second }
                if (fuzzyMatches.isNotEmpty()) {
                    return fuzzyMatches.sortedBy { it.first }.map { it.second }.distinctBy { it.second }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ZoyaTools", "Error finding contact", e)
        }
        return emptyList()
    }

    private fun adjustSystemVolume(direction: String): String {
        val ctx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) context.createAttributionContext("zoya_audio") else context
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val streamType = android.media.AudioManager.STREAM_MUSIC
        try {
            when (direction.lowercase()) {
                "up" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume increased"
                }
                "down" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume decreased"
                }
                "mute" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_MUTE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume muted"
                }
                "unmute" -> {
                    audioManager.adjustStreamVolume(streamType, android.media.AudioManager.ADJUST_UNMUTE, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume unmuted"
                }
                "max" -> {
                    val maxVol = audioManager.getStreamMaxVolume(streamType)
                    audioManager.setStreamVolume(streamType, maxVol, android.media.AudioManager.FLAG_SHOW_UI)
                    return "Volume set to maximum"
                }
                else -> return "Unknown volume direction. Use up, down, mute, or max."
            }
        } catch (e: Exception) {
            return "Failed to adjust volume: \${e.message}"
        }
    }

    private fun toggleTorch(state: String): String {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            if (state.lowercase() == "on") {
                cameraManager.setTorchMode(cameraId, true)
                return "Torch turned on"
            } else {
                cameraManager.setTorchMode(cameraId, false)
                return "Torch turned off"
            }
        } catch (e: Exception) {
            return "Failed to toggle torch: \${e.message}"
        }
    }

    private fun setBrightness(level: Int): String {
        try {
            if (!android.provider.Settings.System.canWrite(context)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = android.net.Uri.parse("package:" + context.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return "Prompted user for write settings permission to change brightness. Please try again after permission is granted."
            }
            
            // Level is 0-100, normalize to 0-255
            val brightness = (level * 255) / 100
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            android.provider.Settings.System.putInt(
                context.contentResolver,
                android.provider.Settings.System.SCREEN_BRIGHTNESS,
                brightness
            )
            return "Brightness set to $level%"
        } catch (e: Exception) {
            return "Failed to set brightness: \${e.message}"
        }
    }

    private fun playMedia(query: String): String {
        try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
            intent.putExtra(android.app.SearchManager.QUERY, query)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "Started playing media for query: $query"
        } catch (e: Exception) {
            return "Failed to play media (no suitable app found): \${e.message}"
        }
    }

    private fun setVolumePercent(percent: Int): String {
        try {
            val ctx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) context.createAttributionContext("zoya_audio") else context
        val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val streamType = android.media.AudioManager.STREAM_MUSIC
            val maxVol = audioManager.getStreamMaxVolume(streamType)
            val targetVol = (maxVol * Math.max(0, Math.min(100, percent))) / 100
            audioManager.setStreamVolume(streamType, targetVol, android.media.AudioManager.FLAG_SHOW_UI)
            return "Volume set to $percent%"
        } catch (e: Exception) {
            return "Failed to set volume: \${e.message}"
        }
    }

    private fun openNotificationPanel(): String {
        try {
            val service = com.example.accessibility.ZoyaAccessibilityService.instance
            if (service != null) {
                val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                if (success) {
                    return "Opened notification panel."
                } else {
                    return "Accessibility service failed to open notification panel."
                }
            } else {
                return "Accessibility service not running. Enable Zoya Automation in Settings > Accessibility."
            }
        } catch (e: Exception) {
            return "Error opening notification panel: \${e.message}"
        }
    }

    private fun sendSms(contactOrNumber: String, message: String): String {
        val isNumber = contactOrNumber.count { it.isDigit() } >= 7 || contactOrNumber.matches(Regex("^[0-9+\\-*#]+$"))
        val number = if (isNumber) {
            contactOrNumber
        } else {
            val matches = findContacts(contactOrNumber)
            if (matches.isEmpty()) return "Could not find a phone number for '$contactOrNumber'. Please specify the number."
            matches.first().second
        }

        val cleanNumber = number.replace(Regex("[^0-9+]"), "")

        // 1. Direct SMS sending via SmsManager if SEND_SMS permission is available
        if (context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(cleanNumber, null, message, null, null)
                }
                return "SMS sent successfully to $contactOrNumber ($cleanNumber): '$message'"
            } catch (e: Exception) {
                android.util.Log.e("ZoyaTools", "Direct SMS sending failed, opening SMS composer", e)
            }
        }

        // 2. Fallback: Open SMS composer and trigger accessibility auto-click send
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$cleanNumber")
            putExtra("sms_body", message)
            putExtra(Intent.EXTRA_TEXT, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            com.example.accessibility.ZoyaAccessibilityService.startSmsSendAutoClick(message)
            context.startActivity(smsIntent)
            return "Opening SMS and sending message to $contactOrNumber ($cleanNumber): '$message'"
        } catch (e: Exception) {
            return "Failed to open SMS app: ${e.message}"
        }
    }

    private fun openWhatsAppChat(contactNameOrNumber: String): String {
        val isNumber = contactNameOrNumber.count { it.isDigit() } >= 7 || contactNameOrNumber.matches(Regex("^[0-9+\\-*#]+$"))
        if (isNumber) {
            val cleanNumber = normalizePhoneNumber(contactNameOrNumber)
            val url = "https://api.whatsapp.com/send?phone=$cleanNumber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                "Opening WhatsApp chat with $contactNameOrNumber."
            } catch (e: Exception) {
                com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(contactNameOrNumber, null, "open")
            }
        } else {
            val matches = findContacts(contactNameOrNumber)
            if (matches.isNotEmpty()) {
                val cleanNumber = normalizePhoneNumber(matches.first().second)
                val url = "https://api.whatsapp.com/send?phone=$cleanNumber"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    return "Opening WhatsApp chat with ${matches.first().first}."
                } catch (e: Exception) {}
            }
            return com.example.accessibility.ZoyaAccessibilityService.startWhatsAppAutomation(contactNameOrNumber, null, "open")
        }
    }

    private fun saveContact(name: String, number: String): String {
        if (name.isBlank() || number.isBlank()) {
            return "Error: Name and phone number cannot be empty."
        }
        
        if (context.checkSelfPermission(android.Manifest.permission.WRITE_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            try {
                val ops = ArrayList<android.content.ContentProviderOperation>()
                
                ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(android.provider.ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(android.provider.ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build())
                    
                ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(android.provider.ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(android.provider.ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build())
                    
                ops.add(android.content.ContentProviderOperation.newInsert(android.provider.ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(android.provider.ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(android.provider.ContactsContract.Data.MIMETYPE, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                    .withValue(android.provider.ContactsContract.CommonDataKinds.Phone.TYPE, android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build())
                    
                context.contentResolver.applyBatch(android.provider.ContactsContract.AUTHORITY, ops)
                return "Contact '$name' with number $number has been successfully saved."
            } catch (e: Exception) {
                android.util.Log.e("ZoyaTools", "ContentProvider save failed, falling back to Intent", e)
            }
        }
        
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = android.provider.ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(android.provider.ContactsContract.Intents.Insert.NAME, name)
            putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, number)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Opening contact saving screen for '$name' ($number)."
        } catch (e: Exception) {
            "Failed to save contact: ${e.message}"
        }
    }

    private fun configureNotificationSettings(args: Map<String, kotlinx.serialization.json.JsonElement>): String {
        val results = mutableListOf<String>()
        args["privacyMode"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()?.let {
            com.example.notification.NotificationManagerHelper.setPrivacyMode(context, it)
            results.add("Privacy Mode: ${if (it) "ON" else "OFF"}")
        }
        args["messagePreviewMode"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()?.let {
            com.example.notification.NotificationManagerHelper.setMessagePreviewEnabled(context, it)
            results.add("Message Preview: ${if (it) "ON" else "OFF"}")
        }
        args["autonomousMode"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()?.let {
            com.example.notification.NotificationManagerHelper.setAutonomousMode(context, it)
            results.add("Autonomous 'Tum Handle Kar Lo' Mode: ${if (it) "ON" else "OFF"}")
        }
        args["replyStyle"]?.jsonPrimitive?.content?.let {
            com.example.notification.NotificationManagerHelper.setReplyStyle(context, it)
            results.add("Reply Style: $it")
        }
        args["monitorWhatsApp"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()?.let {
            com.example.notification.NotificationManagerHelper.setAppMonitoring(context, "whatsapp", it)
            results.add("WhatsApp Monitoring: ${if (it) "ON" else "OFF"}")
        }
        args["monitorSms"]?.jsonPrimitive?.content?.toBooleanStrictOrNull()?.let {
            com.example.notification.NotificationManagerHelper.setAppMonitoring(context, "sms", it)
            results.add("SMS Monitoring: ${if (it) "ON" else "OFF"}")
        }
        return if (results.isNotEmpty()) {
            "Notification settings updated: " + results.joinToString(", ")
        } else {
            "No settings changed."
        }
    }

    private fun getNotificationSettings(): String {
        val privacy = com.example.notification.NotificationManagerHelper.isPrivacyMode(context)
        val preview = com.example.notification.NotificationManagerHelper.isMessagePreviewEnabled(context)
        val autoMode = com.example.notification.NotificationManagerHelper.isAutonomousMode(context)
        val replyStyle = com.example.notification.NotificationManagerHelper.getReplyStyle(context)
        val waMon = com.example.notification.NotificationManagerHelper.isAppMonitored(context, "com.whatsapp")
        val smsMon = com.example.notification.NotificationManagerHelper.isAppMonitored(context, "sms")

        return """
            Notification Settings:
            - Privacy Mode: ${if (privacy) "ON (Only announce app, hide sender & text)" else "OFF (Announce sender)"}
            - Message Preview Mode: ${if (preview) "ON (Read message preview)" else "OFF (Ask before reading)"}
            - Autonomous Reply ('Tum handle kar lo'): ${if (autoMode) "ON (Safe auto-replies enabled for simple chats)" else "OFF (Manual confirmation required)"}
            - Reply Style: $replyStyle
            - WhatsApp Monitoring: ${if (waMon) "ON" else "OFF"}
            - SMS Monitoring: ${if (smsMon) "ON" else "OFF"}
        """.trimIndent()
    }

    private fun answerIncomingCall(): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    telecomManager.acceptRingingCall()
                    return "Incoming call answered."
                } catch (e: Exception) {
                    android.util.Log.e("ZoyaTools", "Failed to answer via TelecomManager", e)
                }
            }
        }
        // Fallback to accessibility click on screen
        val clicked = com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Answer") ||
                com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Accept") ||
                com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Uthao")
        return if (clicked) "Incoming call answered via accessibility." else "Unable to answer call: Permission or UI button not found."
    }

    private fun rejectIncomingCall(): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            if (context.checkSelfPermission(android.Manifest.permission.ANSWER_PHONE_CALLS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                    telecomManager.endCall()
                    return "Incoming call rejected."
                } catch (e: Exception) {
                    android.util.Log.e("ZoyaTools", "Failed to reject via TelecomManager", e)
                }
            }
        }
        // Fallback to accessibility click on screen
        val clicked = com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Decline") ||
                com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Reject") ||
                com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Dismiss") ||
                com.example.accessibility.ZoyaAccessibilityService.clickTextOnScreen("Kaat do")
        return if (clicked) "Incoming call declined." else "Call decline action executed."
    }

    private fun getSimCardInfo(): String {
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "Unable to determine SIM cards because READ_PHONE_STATE permission is lacking. Proceed assuming 1 SIM."
        }
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
            val phoneAccounts = telecomManager.callCapablePhoneAccounts
            return "The device has ${phoneAccounts.size} active calling SIM cards."
        } catch (e: Exception) {
            return "Error determining SIM cards: ${e.message}. Proceed assuming 1 SIM."
        }
    }
}
