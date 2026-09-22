package com.example.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AssistantState {
    IDLE,
    LISTENING,
    VERIFYING_VOICE,
    PLANNING,
    EXECUTING,
    VERIFYING_ACTION,
    WAITING,
    WARNING,
    SECURITY_LOCK_PENDING,
    DEVICE_LOCKED
}

object AssistantCore {
    private const val TAG = "AssistantCore"

    val voiceInputManager = VoiceInputManager
    val voiceVerificationManager = VoiceVerificationManager
    val voiceGuardianManager = VoiceGuardianManager
    val permissionManager = PermissionManager
    val securityManager = SecurityManager
    val deviceLockManager = DeviceLockManager
    val screenControlManager = ScreenControlManager
    val accessibilityController = AccessibilityController
    val screenAnalyzer = ScreenAnalyzer
    val notificationManager = AssistantNotificationManager
    val notificationParser = NotificationParser
    val callManager = CallManager
    val messageManager = MessageManager
    val voiceOutputManager = VoiceOutputManager
    val eventManager = EventManager
    val actionVerificationManager = ActionVerificationManager
    val actionHistoryManager = ActionHistoryManager
    val settingsManager = SettingsManager
    val intentManager = IntentManager

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val appContext = context.applicationContext
        isInitialized = true

        voiceOutputManager.init(appContext)
        securityManager.init(appContext)

        scope.launch {
            securityManager.securityState.collect { secState ->
                when (secState) {
                    SecurityState.NORMAL -> _assistantState.value = AssistantState.IDLE
                    SecurityState.WARNING -> _assistantState.value = AssistantState.WARNING
                    SecurityState.SECURITY_LOCK_PENDING -> _assistantState.value = AssistantState.SECURITY_LOCK_PENDING
                    SecurityState.DEVICE_LOCKED_BY_VOICE_GUARDIAN -> _assistantState.value = AssistantState.DEVICE_LOCKED
                }
            }
        }
        Log.i(TAG, "AssistantCore successfully initialized.")
    }

    /**
     * Executes a command following the mandatory lifecycle:
     * UNDERSTAND -> IDENTIFY USER -> CHECK PERMISSION -> PLAN -> EXECUTE -> OBSERVE -> VERIFY -> RESPOND.
     */
    suspend fun executeCommand(
        context: Context,
        command: String,
        audioSamples: ShortArray? = null,
        speakerHint: String? = null
    ): String {
        Log.i(TAG, "Executing command: '$command'")
        _assistantState.value = AssistantState.PLANNING

        // 1. UNDERSTAND
        val intent = intentManager.parse(command)

        // 2 & 3. IDENTIFY USER & CHECK PERMISSION (Voice Guardian)
        _assistantState.value = AssistantState.VERIFYING_VOICE
        val decision = voiceGuardianManager.evaluateCommand(
            context = context,
            command = command,
            permission = intent.permission,
            audioSamples = audioSamples ?: voiceInputManager.getLastVoiceSample(),
            speakerHint = speakerHint
        )

        when (decision) {
            is GuardianDecision.Blocked -> {
                _assistantState.value = if (decision.isLockTriggered) AssistantState.DEVICE_LOCKED else AssistantState.WARNING
                voiceOutputManager.speakSecurityWarning(decision.reason)
                eventManager.publish(
                    AssistantEvent.UserCommandEvent(
                        command = command,
                        speakerRole = VoiceRole.UNKNOWN,
                        isProtected = true
                    )
                )
                return decision.reason
            }
            is GuardianDecision.Allowed -> {
                // Proceed with execution
            }
        }

        // 4. PLAN & 5. EXECUTE
        _assistantState.value = AssistantState.EXECUTING
        eventManager.publish(
            AssistantEvent.UserCommandEvent(
                command = command,
                speakerRole = (decision as GuardianDecision.Allowed).role,
                isProtected = permissionManager.isProtectedAction(intent.permission)
            )
        )

        // 6 & 7. OBSERVE & VERIFY
        _assistantState.value = AssistantState.VERIFYING_ACTION
        val result: VerificationResult = when (intent) {
            is AssistantIntent.OpenApp -> {
                screenControlManager.openApp(context, intent.appName)
            }
            is AssistantIntent.YouTubePlay -> {
                screenControlManager.searchAndPlayYouTube(context, intent.query)
            }
            is AssistantIntent.YouTubeNav -> {
                screenControlManager.navigateYouTube(intent.action)
            }
            is AssistantIntent.ScreenTap -> {
                screenControlManager.clickOnScreen(intent.targetText)
            }
            is AssistantIntent.ScreenScroll -> {
                screenControlManager.scrollScreen(intent.direction)
            }
            is AssistantIntent.DeviceLock -> {
                screenControlManager.lockDevice(context)
            }
            is AssistantIntent.DeviceUnlock -> {
                MayaLockManager.executeUnlockWorkflow(context)
                VerificationResult.Success("Maya unlock workflow initiate ho gaya.")
            }
            is AssistantIntent.BuildWebsite -> {
                val (success, message) = com.example.model.WebsiteSynthesisEngine.synthesizeAndLaunch(
                    context = context,
                    prompt = intent.topic,
                    openInChrome = true
                )
                if (success) {
                    VerificationResult.Success(message)
                } else {
                    VerificationResult.Failure(message)
                }
            }
            is AssistantIntent.AnswerCall -> {
                if (intent.isAccept) callManager.answerIncomingCall(context)
                else callManager.rejectIncomingCall(context)
            }
            is AssistantIntent.MakeCall -> {
                val engine = com.example.tools.ToolExecutionEngine(context)
                val out = engine.execute("searchAndCallContact", kotlinx.serialization.json.buildJsonObject {
                    put("contactName", kotlinx.serialization.json.JsonPrimitive(intent.contact))
                })
                VerificationResult.Success(out)
            }
            is AssistantIntent.SendWhatsApp -> {
                val engine = com.example.tools.ToolExecutionEngine(context)
                val out = engine.execute("sendWhatsAppMessage", kotlinx.serialization.json.buildJsonObject {
                    put("contactName", kotlinx.serialization.json.JsonPrimitive(intent.contact))
                    put("message", kotlinx.serialization.json.JsonPrimitive(intent.message))
                })
                VerificationResult.Success(out)
            }
            is AssistantIntent.SendSms -> {
                val engine = com.example.tools.ToolExecutionEngine(context)
                val out = engine.execute("sendSmsMessage", kotlinx.serialization.json.buildJsonObject {
                    put("contactNameOrNumber", kotlinx.serialization.json.JsonPrimitive(intent.contact))
                    put("message", kotlinx.serialization.json.JsonPrimitive(intent.message))
                })
                VerificationResult.Success(out)
            }
            is AssistantIntent.SecurityConfig,
            is AssistantIntent.VoiceConfig,
            is AssistantIntent.GeneralChat -> {
                VerificationResult.Success("")
            }
        }

        // 8. RESPOND (Only verified confirmations!)
        _assistantState.value = AssistantState.IDLE
        return when (result) {
            is VerificationResult.Success -> {
                if (result.message.isNotEmpty()) {
                    actionHistoryManager.recordAction(
                        actionName = intent.javaClass.simpleName,
                        target = command,
                        success = true,
                        message = result.message
                    )
                    eventManager.publish(
                        AssistantEvent.ActionResultEvent(
                            actionName = intent.javaClass.simpleName,
                            target = command,
                            success = true,
                            message = result.message
                        )
                    )
                    voiceOutputManager.speakActionResult(intent.javaClass.simpleName, true, result.message)
                }
                result.message
            }
            is VerificationResult.Failure -> {
                actionHistoryManager.recordAction(
                    actionName = intent.javaClass.simpleName,
                    target = command,
                    success = false,
                    message = result.reason
                )
                eventManager.publish(
                    AssistantEvent.ActionResultEvent(
                        actionName = intent.javaClass.simpleName,
                        target = command,
                        success = false,
                        message = result.reason
                    )
                )
                voiceOutputManager.speakActionResult(intent.javaClass.simpleName, false, result.reason)
                result.reason
            }
            is VerificationResult.InProgress -> {
                voiceOutputManager.speak(result.message)
                result.message
            }
            is VerificationResult.Unconfirmed -> {
                voiceOutputManager.speak(result.message)
                result.message
            }
        }
    }
}
