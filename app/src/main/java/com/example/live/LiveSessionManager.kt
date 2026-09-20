package com.example.live

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.tools.ToolExecutionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.decodeBase64
import java.util.concurrent.TimeUnit

enum class ZoyaState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class LiveSessionManager(
    private val context: Context,
    private val toolEngine: ToolExecutionEngine,
    private val onAudioOut: (ByteArray) -> Unit,
    private val onInterrupt: () -> Unit = {}
) {
    private val _zoyaState = MutableStateFlow(ZoyaState.IDLE)
    val zoyaState: StateFlow<ZoyaState> = _zoyaState.asStateFlow()

    private val _messages = MutableStateFlow<List<String>>(emptyList())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private val json = Json { ignoreUnknownKeys = true }
    
    // Tools definition
    private val toolsJson = buildJsonObject {
        putJsonArray("functionDeclarations") {
            add(buildJsonObject {
                put("name", "openApp")
                put("description", "Open an application package, like WhatsApp or YouTube")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("packageName") {
                            put("type", "STRING")
                            put("description", "A generic name of the app to launch (e.g. 'WhatsApp', 'YouTube', 'Settings', 'Calculator')")
                        }
                    }
                    putJsonArray("required") { add("packageName") }
                }
            })
            add(buildJsonObject {
                put("name", "searchAndCallContact")
                put("description", "Search for a contact name on the device and call them. Can optionally open dialer instead of calling immediately, or use a specific SIM card slot.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The EXACT name of the contact as spoken by the user. NEVER guess or invent numbers. If the user says a name, use exactly that name.")
                        }
                        putJsonObject("useDialer") {
                            put("type", "BOOLEAN")
                            put("description", "Set to true if user wants to open dial pad / keyboard so they can see the number before calling")
                        }
                        putJsonObject("simSlot") {
                            put("type", "INTEGER")
                            put("description", "1 for SIM 1, 2 for SIM 2 if user specified. Null if default.")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "sendWhatsAppMessage")
                put("description", "Send a WhatsApp message to a specific contact with some text.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The EXACT name of the contact as spoken by the user. NEVER guess or invent numbers. If the user says a name, use exactly that name.")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                        }
                    }
                    putJsonArray("required") { add("contactName"); add("message") }
                }
            })
            add(buildJsonObject {
                put("name", "sendGmail")
                put("description", "Draft or send an email.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("recipientEmail") { put("type", "STRING") }
                        putJsonObject("subject") { put("type", "STRING") }
                        putJsonObject("body") { put("type", "STRING") }
                    }
                    putJsonArray("required") { add("recipientEmail"); add("subject"); add("body") }
                }
            })
            add(buildJsonObject {
                put("name", "searchYouTube")
                put("description", "Search and automatically play songs, music playlists, or videos on the YouTube app. Use this when the user asks to play a song on YouTube, play a Hindi song, play any good song ('koi bhi achha song chala de'), or search for a video on YouTube.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") { 
                            put("type", "STRING") 
                            put("description", "The search query or song title (e.g. 'Trending Hit Hindi Songs 2026', 'Arijit Singh Top Songs', 'Kesariya', or user specified title/artist).")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "adjustVolume")
                put("description", "Adjust the device volume.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("direction") { 
                            put("type", "STRING") 
                            put("description", "Volume action: 'up', 'down', 'mute', 'unmute', or 'max'")
                        }
                    }
                    putJsonArray("required") { add("direction") }
                }
            })
            add(buildJsonObject {
                put("name", "setVolumePercent")
                put("description", "Set the device volume to a specific percentage (0 to 100).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("percent") { 
                            put("type", "INTEGER") 
                            put("description", "Volume percentage (0-100)")
                        }
                    }
                    putJsonArray("required") { add("percent") }
                }
            })
            add(buildJsonObject {
                put("name", "getSimCardInfo")
                put("description", "Check how many active SIM cards the device has.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "openQuickSettings")
                put("description", "Pull down the quick settings / components panel (toggles for wifi, bluetooth, etc).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "clickTextOnScreen")
                put("description", "Click on any text visible on the screen. Acts like a real human finger tap and shows tap effect visually.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("text") {
                            put("type", "STRING")
                            put("description", "The text to tap on the screen")
                        }
                    }
                    putJsonArray("required") { add("text") }
                }
            })
            add(buildJsonObject {
                put("name", "openNotificationPanel")
                put("description", "Pull down the notification bar / status bar to view notifications.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "toggleTorch")
                put("description", "Turn the flashlight/torch on or off.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("state") { 
                            put("type", "STRING") 
                            put("description", "'on' or 'off'")
                        }
                    }
                    putJsonArray("required") { add("state") }
                }
            })
            add(buildJsonObject {
                put("name", "setBrightness")
                put("description", "Set the screen brightness. Note: Requires write settings permission first.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("level") { 
                            put("type", "INTEGER") 
                            put("description", "Brightness level 0 to 100")
                        }
                    }
                    putJsonArray("required") { add("level") }
                }
            })
            add(buildJsonObject {
                put("name", "playMedia")
                put("description", "Play media (like a song, video, or movie) from another app by searching for it.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("query") { 
                            put("type", "STRING") 
                            put("description", "What to play (e.g. 'Despacito by Luis Fonsi' or 'latest tech news')")
                        }
                    }
                    putJsonArray("required") { add("query") }
                }
            })
            add(buildJsonObject {
                put("name", "readScreenText")
                put("description", "Read and inspect all text and clickable buttons currently visible on the user's phone screen. Use this tool when the user asks you to read, see, look at, or analyze their screen, or when you need screen contents to answer questions or help them.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "readLastNotification")
                put("description", "Read the last received system notification (e.g., incoming messages on WhatsApp, Instagram, SMS, etc.). Call this when asked what message or notification was received, or when checking if there are new messages.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "whatsappSearchAndMessage")
                put("description", "Search for a contact by name inside WhatsApp and send them a message automatically. Use this when the user asks to send a WhatsApp message to a specific contact name.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The name of the contact to search for in WhatsApp.")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "The message text to send.")
                        }
                    }
                    putJsonArray("required") {
                        add("contactName")
                        add("message")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "whatsappSearchAndCall")
                put("description", "Search for a contact by name inside WhatsApp and initiate a WhatsApp voice call automatically.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The name of the contact to search and call on WhatsApp.")
                        }
                    }
                    putJsonArray("required") {
                        add("contactName")
                    }
                }
            })
            add(buildJsonObject {
                put("name", "scrollScreen")
                put("description", "Scroll the screen in a specified direction (down, up, left, or right). Useful for scrolling Reels, Shorts, web pages, lists, or any other content when the user asks you to scroll, swipe, or show the next/previous post.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("direction") {
                            put("type", "STRING")
                            put("description", "The direction to scroll: 'down' (to see content below, like scrolling to next reel), 'up' (to see content above), 'left', or 'right'. Default is 'down'.")
                            putJsonArray("enum") {
                                add("down")
                                add("up")
                                add("left")
                                add("right")
                            }
                        }
                    }
                }
            })
            add(buildJsonObject {
                put("name", "rememberFact")
                put("description", "Save any fact, personal detail, contact detail, credential, or information permanently in the assistant's long-term memory so Zoya never forgets it.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("topic") {
                            put("type", "STRING")
                            put("description", "The topic or label of what to remember (e.g. 'Car Number', 'WiFi Password', 'Boss Birthday', 'Friend Shivank')")
                        }
                        putJsonObject("fact") {
                            put("type", "STRING")
                            put("description", "The exact fact or information to remember permanently.")
                        }
                    }
                    putJsonArray("required") { add("topic"); add("fact") }
                }
            })
            add(buildJsonObject {
                put("name", "teachSkill")
                put("description", "Teach Zoya a new custom routine, rule, or behavior triggered by a specific voice command or phrase from the boss.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("trigger") {
                            put("type", "STRING")
                            put("description", "The trigger phrase or command taught by user (e.g. 'Good night', 'Office mode', 'Emergency report')")
                        }
                        putJsonObject("actionOrRule") {
                            put("type", "STRING")
                            put("description", "The exact action or instructions to execute whenever this trigger is spoken.")
                        }
                    }
                    putJsonArray("required") { add("trigger"); add("actionOrRule") }
                }
            })
            add(buildJsonObject {
                put("name", "getLearnedMemories")
                put("description", "Retrieve all permanent memories, remembered facts, and taught skills.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "forgetMemory")
                put("description", "Remove a specific remembered fact or taught skill from permanent memory.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("key") {
                            put("type", "STRING")
                            put("description", "The topic or trigger phrase to forget.")
                        }
                    }
                    putJsonArray("required") { add("key") }
                }
            })
            add(buildJsonObject {
                put("name", "sendSmsMessage")
                put("description", "Send an SMS / text message to a contact or phone number.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactNameOrNumber") {
                            put("type", "STRING")
                            put("description", "The contact name or phone number.")
                        }
                        putJsonObject("message") {
                            put("type", "STRING")
                            put("description", "The text message content.")
                        }
                    }
                    putJsonArray("required") { add("contactNameOrNumber"); add("message") }
                }
            })
            add(buildJsonObject {
                put("name", "findContact")
                put("description", "Search the device contacts by name, nickname, title (e.g. 'Kamlesh Sir', 'Mummy', 'Papa', 'Rahul'), or partial name to find their phone number and check matches.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The name of the contact to find.")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "getContactNumbers")
                put("description", "Get the phone number(s) of a specific contact from the device address book.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactName") {
                            put("type", "STRING")
                            put("description", "The contact name whose number is requested.")
                        }
                    }
                    putJsonArray("required") { add("contactName") }
                }
            })
            add(buildJsonObject {
                put("name", "openWhatsAppChat")
                put("description", "Open the WhatsApp chat conversation screen for a contact or phone number without typing or sending any message.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("contactNameOrNumber") {
                            put("type", "STRING")
                            put("description", "The contact name or phone number.")
                        }
                    }
                    putJsonArray("required") { add("contactNameOrNumber") }
                }
            })
            add(buildJsonObject {
                put("name", "saveContact")
                put("description", "Save a new contact with a name and phone number to the Android phone's contacts system.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("name") {
                            put("type", "STRING")
                            put("description", "The full name of the contact to save.")
                        }
                        putJsonObject("number") {
                            put("type", "STRING")
                            put("description", "The phone number to save.")
                        }
                    }
                    putJsonArray("required") { add("name"); add("number") }
                }
            })
            add(buildJsonObject {
                put("name", "setConfirmationMode")
                put("description", "Toggle confirmation mode on (ask before sending messages/calls) or off (instant mode: execute immediately without asking).")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("enabled") {
                            put("type", "BOOLEAN")
                            put("description", "True for Confirmation mode ON, False for Instant mode ON.")
                        }
                    }
                    putJsonArray("required") { add("enabled") }
                }
            })
            add(buildJsonObject {
                put("name", "getRecentNotifications")
                put("description", "Retrieve incoming notifications and communication events (WhatsApp messages, SMS, missed/incoming calls) intercepted by Zoya.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "configureNotificationSettings")
                put("description", "Update notification, privacy, or autonomous reply settings based on user commands.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("privacyMode") {
                            put("type", "BOOLEAN")
                            put("description", "True to hide sender names and message previews from spoken announcements.")
                        }
                        putJsonObject("messagePreviewMode") {
                            put("type", "BOOLEAN")
                            put("description", "True to read notification preview text aloud, false to ask first.")
                        }
                        putJsonObject("autonomousMode") {
                            put("type", "BOOLEAN")
                            put("description", "True for 'Tum handle kar lo' mode (auto-reply to simple routine chats), false to ask every time.")
                        }
                        putJsonObject("replyStyle") {
                            put("type", "STRING")
                            put("description", "Reply style: 'casual', 'professional', 'short', 'friendly', or 'formal'.")
                        }
                        putJsonObject("monitorWhatsApp") {
                            put("type", "BOOLEAN")
                            put("description", "Enable or disable WhatsApp notification monitoring.")
                        }
                        putJsonObject("monitorSms") {
                            put("type", "BOOLEAN")
                            put("description", "Enable or disable SMS notification monitoring.")
                        }
                    }
                }
            })
            add(buildJsonObject {
                put("name", "getNotificationSettings")
                put("description", "View current notification monitoring, privacy, and autonomous reply settings.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "answerIncomingCall")
                put("description", "Answer an incoming phone call or WhatsApp call when instructed by user (e.g. 'Utha lo', 'Call pick kar').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "rejectIncomingCall")
                put("description", "Reject or decline an incoming phone call or WhatsApp call (e.g. 'Reject kar do', 'Kaat do').")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "playFavoriteSong")
                put("description", "Play user's favorite song on their preferred music app (YT Music, Spotify, or YouTube) configured in Settings.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
            add(buildJsonObject {
                put("name", "triggerSosEmergency")
                put("description", "Trigger emergency SOS call to the favorite SOS contacts saved in Settings.")
                putJsonObject("parameters") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {}
                }
            })
        }
    }

    fun startSession() {
        if (webSocket != null) return
        
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        if (apiKey.isEmpty()) {
            addMessage("Error: API Key is missing. Please set it in Settings.")
            _zoyaState.value = ZoyaState.IDLE
            return
        }
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY") {
            Log.e("ZoyaDiagnostic", "No API Key found")
            addMessage("Error: Gemini API Key is missing. Please add it to the Secrets tab.")
            return
        }
        
        Log.i("ZoyaDiagnostic", "Connecting to Gemini Live API...")
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("ZoyaDiagnostic", "WebSocket connection OPENED successfully.")
                addMessage("WebSocket Opened")
                isSetupComplete = false
                sendSetupMessage(webSocket)
                _zoyaState.value = ZoyaState.LISTENING
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("ZoyaDiagnostic", "WebSocket Text Msg Received (length: ${text.length})")
                handleServerMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                val text = bytes.utf8()
                Log.d("ZoyaDiagnostic", "WebSocket Binary Msg Received (utf8 length: ${text.length})")
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorBody = response?.body?.string() ?: "No body"
                Log.e("ZoyaDiagnostic", "WebSocket ERROR: ${t.message}, Response: $errorBody", t)
                addMessage("WebSocket Error: ${t.message}. Details: $errorBody")
                _zoyaState.value = ZoyaState.IDLE
                this@LiveSessionManager.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("ZoyaDiagnostic", "WebSocket CLOSED. Code: $code, Reason: $reason")
                addMessage("WebSocket Closed: $reason")
                _zoyaState.value = ZoyaState.IDLE
                this@LiveSessionManager.webSocket = null
            }
        })
    }

    private fun sendInitialPrompt(ws: WebSocket) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val bossName = prefs.getString("boss_name", "Boss") ?: "Boss"
        val msg = buildJsonObject {
            putJsonObject("clientContent") {
                putJsonArray("turns") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject {
                                put("text", "Hi Zoya! Greet me in Hindi and address me by my boss name: $bossName. Keep it brief and friendly.")
                            })
                        }
                    })
                }
                put("turnComplete", true)
            }
        }
        ws.send(msg.toString())
    }

    private fun addMessage(msg: String) {
        _messages.value = _messages.value + msg
    }

    fun stopSession() {
        webSocket?.close(1000, "User stopped")
        webSocket = null
        _zoyaState.value = ZoyaState.IDLE
        addMessage("Session stopped.")
    }

    fun sendTextMessage(text: String) {
        if (webSocket == null || !isSetupComplete || _zoyaState.value == ZoyaState.IDLE) return
        addMessage("You: $text")
        val msg = buildJsonObject {
            putJsonObject("clientContent") {
                putJsonArray("turns") {
                    add(buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", text) })
                        }
                    })
                }
                put("turnComplete", true)
            }
        }
        webSocket?.send(msg.toString())
    }
    
    fun sendAudioData(pcmData: ShortArray, length: Int) {
        if (webSocket == null || !isSetupComplete || _zoyaState.value == ZoyaState.IDLE) {
            return
        }
        
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val echoGuard = prefs.getBoolean("echo_guard", true)
        if (echoGuard && _zoyaState.value == ZoyaState.SPEAKING) {
            // Echo guard active: Mute the mic while Maya speaks
            return
        }
        
        Log.v("ZoyaDiagnostic", "Sending audio chunk size=${length} to Gemini")
        // Convert ShortArray to ByteArray (Little Endian)
        val byteArray = ByteArray(length * 2)
        for (i in 0 until length) {
            val s = pcmData[i]
            byteArray[i * 2] = (s.toInt() and 0x00FF).toByte()
            byteArray[i * 2 + 1] = (s.toInt() shr 8).toByte()
        }
        
        val base64Data = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        
        val inputMsg = buildJsonObject {
            putJsonObject("realtimeInput") {
                putJsonArray("mediaChunks") {
                    add(buildJsonObject {
                        put("mimeType", "audio/pcm;rate=16000")
                        put("data", base64Data)
                    })
                }
            }
        }
        webSocket?.send(inputMsg.toString())
    }
    
    private fun sendSetupMessage(ws: WebSocket) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", android.content.Context.MODE_PRIVATE)
        val bossName = prefs.getString("boss_name", "RDX sir") ?: "RDX sir"
        val assistantName = prefs.getString("assistant_name", "MAYA") ?: "MAYA"
        val musicApp = prefs.getString("preferred_music_app", "YT Music") ?: "YT Music"
        val favoriteSong = prefs.getString("favorite_song", "") ?: ""
        val appLanguage = prefs.getString("app_language", "Hinglish (Hindi + English) — default") ?: "Hinglish (Hindi + English) — default"
        val countryCode = prefs.getString("country_code", "India (+91)") ?: "India (+91)"
        val confirmationMode = prefs.getBoolean("confirmation_mode", false)
        val memoriesText = com.example.memory.MemoryManager.getMemoriesSummary(context)
        val selectedPersona = com.example.persona.PersonaManager.getSelectedPersona(context)
        val personaPrompt = com.example.persona.PersonaManager.getPersonaPrompt(selectedPersona, assistantName, bossName, appLanguage)

        val systemPrompt = """
You are $assistantName, an advanced, hands-free personal Android AI Assistant created by The Shadow X Rahul AI.
Your communication system is completely natural-language driven.
Preferred Language: $appLanguage.
Default Country Code: $countryCode.
Preferred Music App: $musicApp (Favorite Song: $favoriteSong).
Always address the user warmly as your Boss / Sir (Name: $bossName).

$personaPrompt

${if (appLanguage.contains("Bhojpuri", ignoreCase = true)) """
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
BHOJPURI LANGUAGE INSTRUCTION:
- Respond naturally, warmly and respectfully in pure/natural Bhojpuri (भोजपुरी).
- Respectfully use honorifics like "रउआ", "मालिक", "राउर".
- Common Bhojpuri phrases for tool responses:
  • Greeting / Ready: "प्रणाम मालिक $bossName, का हुकुम बा?", "जी मालिक, बताईं का काम बा?"
  • Message sent: "मेसेज भेज देहली मालिक।"
  • Phone Call: "$bossName, फोन लगावत बानी।"
  • WhatsApp Call: "व्हाट्सएप कॉल लगावत बानी।"
  • Favorite Song: "राउर पसंदीदा गाना बजावत बानी मालिक।"
  • Contact saved: "नंबर सेव हो गइल।"
  • Contact not found: "मालिक, नंबर ना मिलल, एगो बेर फेर से नाम बताईं।"
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
""" else ""}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
1. NATURAL LANGUAGE UNDERSTANDING (MOST CRITICAL)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- The user does NOT need to use fixed commands, exact keywords, or predefined templates.
- The user can speak casually in Bhojpuri, Hindi, Hinglish, English, slang, short phrases, incomplete sentences, or mixed word orders.
- Examples of same intent:
  "Kamlesh Sir ko WhatsApp kar de."
  "कमलेश सर के व्हाट्सएप पर मेसेज भेज द।"
  "Kamlesh Sir ko WhatsApp pe message kar."
  "Kamlesh Sir ko ek message bhej."
  "Kamlesh Sir ko likh de ki kal class hai."
  "कमलेश सर के बोल द की काल्ह 8 बजे आवे के बा।"
  "Are Kamlesh Sir ko bol dena kal 8 baje aana hai."
  "Kamlesh Sir ko bata de ki kal class 8 baje hai."
  "Yaar Kamlesh Sir ko WhatsApp kar de, kal jaldi aa jaye."
  "Kamlesh Sir ko text karna."
  "WhatsApp pe Kamlesh Sir ko bol de."
- Infer the intended action and parameters directly from context. NEVER require exact tool keywords.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
2. INTENT & MESSAGE EXTRACTION
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
For every communication request, extract:
- CONTACT: Contact name, nickname, title ("Kamlesh Sir", "Mummy", "Papa", "Bhai", "Pankaj Sir", "Rahul", "Chacha", etc.)
- ACTION & APP:
  • WhatsApp Message: 'whatsappSearchAndMessage' (or 'sendWhatsAppMessage')
  • WhatsApp Call: 'whatsappSearchAndCall'
  • Normal Phone Call: 'searchAndCallContact'
  • SMS / Text: 'sendSmsMessage'
  • Open Chat Screen: 'openWhatsAppChat'
  • Get Contact Info: 'getContactNumbers' / 'findContact'
  • Save Contact: 'saveContact'
- MESSAGE CONTENT: Extract ONLY the actual intended message. STRIP OUT command wrappers like "bol de ki", "message bhej", "WhatsApp kar dena", "likh de ki", "bata de ki", "text kar", etc.
  Example: "Kamlesh Sir ko bol de ki main aaj coaching nahi aa paunga." -> Message: "Main aaj coaching nahi aa paunga."

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
3. CONTEXT MEMORY, PRONOUNS & FOLLOW-UPS
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Maintain conversation context across turns.
- Resolve pronouns naturally: "usko", "unko", "inko", "ise", "wahi", "same", "ye message", "pichla message", "pehle wale ko".
- Reuse previous messages when commanded: "Jo abhi Kamlesh Sir ko bheja wahi Pankaj Sir ko bhi bhej" / "Same message mummy ko bhi bhej".
- Support message editing before or on follow-up: "Usme kal ki jagah parso kar de" / "Time 8 se 9 kar de" -> Update the message text and send.
- Multi-action commands: If user asks for multiple actions in one sentence (e.g., "Kamlesh Sir ko WhatsApp kar ki kal class hai aur phir unko call bhi kar dena"), execute sequentially.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
4. SMART APP SELECTION & AMBIGUITY
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- If app is explicitly specified ("WhatsApp pe...", "SMS kar...", "WhatsApp call...", "Phone kar...", "YouTube par..."), use that exact app.
- If user asks to play music/songs on YouTube or says "YouTube par Hindi song play kar", "koi bhi achha song chala de", "YouTube pe gaana bajao", "Arijit Singh ka gana chala do":
  • Formulate a top trending query (e.g. "Trending Hindi Hit Songs 2026", "Best Hindi Songs", or user's requested track).
  • Call 'searchYouTube' immediately with that query.
- If app is NOT specified:
  • For "message/text/likh/bata/bol" -> Default to WhatsApp ('whatsappSearchAndMessage').
  • For "call/phone/mila" -> Default to normal phone call ('searchAndCallContact').
  • For "gaana bajao/play song" -> Default to Preferred Music App ($musicApp) or 'searchYouTube'.
- DO NOT OVER-ASK. If the user says "Kamlesh Sir ko WhatsApp kar de ki main late ho jaunga", DO NOT ask "Kis app par?". Execute immediately.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
5. EXECUTION & CONFIRMATION MODES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Current Confirmation Mode: ${if (confirmationMode) "ON (Confirm before sending)" else "OFF (Instant Mode / Execute Immediately)"}
- If Confirmation Mode is OFF (Instant Mode): Execute the action IMMEDIATELY without asking "Should I send this?".
- If Confirmation Mode is ON: State the recipient and message/call, ask once ("Kamlesh Sir ko ye message bhej doon?"), and execute upon confirmation.
- User can switch modes anytime: "Confirmation mode on" / "Confirm karke bhejna" -> call 'setConfirmationMode' with enabled=true. "Instant mode on" / "Bina confirmation ke karna" -> call 'setConfirmationMode' with enabled=false.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
6. SHORT, HUMAN VOICE RESPONSES
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- DO NOT output internal planning, thinking, or step-by-step narration. Call tools directly in silence!
- Keep voice responses short, crisp, natural, and friendly:
  • Message sent: "Message bhej diya."
  • WhatsApp call started: "WhatsApp call laga raha hoon."
  • Phone call started: "Call laga raha hoon."
  • Chat opened: "Chat khol raha hoon."
  • Contact saved: "Contact save kar diya."
  • Contact not found: "Kamlesh Sir ka contact nahi mila. Unka number bata do ya naam dobara bata do."
  • Multiple contacts found: "Is naam ke multiple contacts mile hain. Kis wale ko message/call karna hai?"
  • Permission missing: "Contacts permission chahiye. Settings se allow kar do."
- Never claim success if a tool reports an error or failure.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
7. PERMANENT LONG-TERM MEMORY & TEACHING ENGINE
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- If Boss says 'Yaad rakhna...', 'Mera xyz ye hai...', 'Note down...': Call 'rememberFact' immediately. Verbally say: "Ji $bossName, maine yaad rakh liya hai."
- If Boss teaches a skill/rule ('Jab main bolu Good night to...'): Call 'teachSkill' immediately. Verbally say: "Ji $bossName, maine seekh liya hai."
- Stored memories & learned skills:
$memoriesText

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
8. INCOMING COMMUNICATION & NOTIFICATION MANAGER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Intercept incoming communication via 'getRecentNotifications'.
- Spoken Announcement Format:
  • WhatsApp: "$bossName, [Sender] ne WhatsApp par message kiya hai."
  • SMS: "$bossName, [Sender] ne SMS kiya hai."
  • Phone Call: "$bossName, [Sender] ka call aa raha hai."
  • WhatsApp Call: "$bossName, [Sender] ka WhatsApp call aa raha hai."
- Privacy Mode:
  • If Privacy Mode is ON: Only announce "$bossName, WhatsApp par message aaya hai." (Do NOT speak sender name or message content unless requested).
  • If Privacy Mode is OFF: Announce sender name.
- Message Previews:
  • If preview is available & enabled: "$bossName, [Sender] ne message kiya hai: '[Preview]'. Reply dena hai?"
  • If user says "Ha" / "Reply kar do": Ask "Kya reply bhejna hai?" -> User speaks reply -> execute reply.
- Incoming Call Commands:
  • "Utha lo" / "Pick up" -> Call 'answerIncomingCall'.
  • "Reject kar do" / "Kaat do" -> Call 'rejectIncomingCall'.
  • "Baad mein call kar dena" -> Save reminder via 'rememberFact'.
- Notification Settings Commands:
  • "Privacy mode on/off", "Message preview on/off", "Autonomous reply on/off", "WhatsApp notifications off", "SMS notifications on" -> Call 'configureNotificationSettings'.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
9. "TUM HANDLE KAR LO" (AUTONOMOUS REPLY MODE)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- When Boss says "Tum handle kar lo", "Ab tum dekh lo", "Iska reply tum kar do", "Jo sahi lage reply kar dena", or "Autonomous mode on":
  1. Call 'configureNotificationSettings' with autonomousMode=true.
  2. For simple, safe messages (e.g. "Kal class aaoge?", "Kaha ho?"): Auto-generate and send a polite reply matching Boss's style.
  3. ⚠️ STRICT SAFETY LIMIT: NEVER auto-agree to financial commitments, payments (e.g. ₹500, paytm, transfer), OTPs, passwords, contracts, or sensitive personal data.
     In such cases, ALWAYS stop and ask: "$bossName, is message mein payment/sensitive data ki baat hai. Aap confirm karoge?"

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
10. SCREEN VISION, UI AUTOMATION & STUDY SOLVER
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Screen Vision & Reading: When Boss asks "Screen dekho", "Screen par kya hai", "Screen dekhkar batao", "Screen padho", "Screen dekhkar samjhao":
  1. Call 'readScreenText' immediately to inspect all visible text, headers, buttons, and state.
  2. Answer Boss's question directly based on the screen content.
  3. If the screen contains study material, an exam question, or homework: explain the concept step-by-step thoroughly with simple intuition, formulas, and the correct answer.

- Voice UI Tap Automation ("Boss jaha bole tap karo"): When Boss says "Is par tap karo", "Search par tap karo", "Play par tap karo", "Next / Submit / Allow / Settings / Skip par tap kar do", or tells you to click any button, link, or label on screen:
  1. Call 'clickTextOnScreen' with the target text.
  2. Confirm smoothly: "[Target] par tap kar diya."

- Scrolling: 'scrollScreen' (direction: down/up/left/right).
- Notification check: 'readLastNotification' / 'getRecentNotifications'.
- Device controls: 'toggleTorch', 'setBrightness', 'setVolumePercent', 'adjustVolume', 'openApp', 'playMedia', 'openNotificationPanel', 'openQuickSettings', 'clickTextOnScreen', 'readScreenText'.
""".trimIndent()

        val setupMsg = buildJsonObject {
            putJsonObject("setup") {
                put("model", "models/gemini-2.5-flash-native-audio-latest")
                putJsonObject("generationConfig") {
                    putJsonArray("responseModalities") { add("AUDIO") }
                    putJsonObject("speechConfig") {
                        putJsonObject("voiceConfig") {
                            putJsonObject("prebuiltVoiceConfig") {
                                put("voiceName", "Aoede")
                            }
                        }
                    }
                }
                putJsonObject("systemInstruction") {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", systemPrompt)
                        })
                    }
                }
                putJsonArray("tools") {
                    add(toolsJson)
                }
            }
        }
        ws.send(setupMsg.toString())
    }

    private var isSetupComplete = false

    private fun handleServerMessage(text: String) {
        Log.d("LiveSessionManager", "Server msg: $text")
        try {
            val jsonMsg = json.parseToJsonElement(text).jsonObject
            // DEBUUGING: show keys on UI
            addMessage("Server says: ${jsonMsg.keys}")
            
            if (jsonMsg.containsKey("setupComplete")) {
                isSetupComplete = true
                addMessage("Server says: Setup Complete")
                sendInitialPrompt(webSocket!!)
            }
            if (jsonMsg.containsKey("serverContent")) {
                val serverContent = jsonMsg["serverContent"]?.jsonObject
                val modelTurn = serverContent?.get("modelTurn")?.jsonObject
                
                if (serverContent?.get("interrupted")?.jsonPrimitive?.content == "true" || serverContent?.get("interrupted")?.jsonPrimitive?.booleanOrNull == true) {
                    onInterrupt()
                }
                
                modelTurn?.get("parts")?.jsonArray?.forEach { partElement ->
                    val part = partElement.jsonObject
                    
                    if (part.containsKey("inlineData")) {
                       val dataBase64 = part["inlineData"]?.jsonObject?.get("data")?.jsonPrimitive?.content
                       if (dataBase64 != null) {
                           _zoyaState.value = ZoyaState.SPEAKING
                           val rawBytes = Base64.decode(dataBase64, Base64.NO_WRAP)
                           onAudioOut(rawBytes)
                       }
                    }

                    if (part.containsKey("text")) {
                        val textContent = part["text"]?.jsonPrimitive?.content
                        if (!textContent.isNullOrBlank()) {
                            addMessage("Zoya: $textContent")
                        }
                    }
                }
                
                if (serverContent?.containsKey("turnComplete") == true && serverContent["turnComplete"]?.jsonPrimitive?.content == "true") {
                    _zoyaState.value = ZoyaState.LISTENING
                }
            }
            
            if (jsonMsg.containsKey("toolCall")) {
                val toolCallObj = jsonMsg["toolCall"]?.jsonObject
                val functionCalls = toolCallObj?.get("functionCalls")?.jsonArray
                
                functionCalls?.forEach { callElement ->
                    val callObj = callElement.jsonObject
                    val id = callObj["id"]?.jsonPrimitive?.content ?: ""
                    val name = callObj["name"]?.jsonPrimitive?.content ?: ""
                    val args = callObj["args"]?.jsonObject ?: buildJsonObject { }
                    
                    executeToolAndRespond(id, name, args)
                }
            }
        } catch (e: Exception) {
            Log.e("LiveSessionManager", "Error parsing server message", e)
            addMessage("Parsing error: ${e.message}")
        }
    }
    
    private fun executeToolAndRespond(id: String, name: String, args: JsonObject) {
         _zoyaState.value = ZoyaState.THINKING
         scope.launch {
              val resultStr = toolEngine.execute(name, args)
              
              val responseMsg = buildJsonObject {
                  putJsonObject("toolResponse") {
                      putJsonArray("functionResponses") {
                          add(buildJsonObject {
                              put("id", id)
                              put("name", name)
                              putJsonObject("response") {
                                  put("result", resultStr)
                              }
                          })
                      }
                  }
              }
              webSocket?.send(responseMsg.toString())
         }
    }
}
