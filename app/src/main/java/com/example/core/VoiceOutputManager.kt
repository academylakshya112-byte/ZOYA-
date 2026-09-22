package com.example.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.ZoyaForegroundService
import kotlinx.coroutines.*
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

enum class SpeechPriority(val value: Int) {
    BACKGROUND(0),
    NOTIFICATION(1),
    NORMAL(2),
    ACTION_RESULT(3),
    SECURITY_WARNING(4),
    CALL_ANNOUNCEMENT(5)
}

data class SpeechRequest(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val priority: SpeechPriority,
    val onComplete: (() -> Unit)? = null
)

object VoiceOutputManager {
    private const val TAG = "VoiceOutputManager"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val isCurrentlySpeaking = AtomicBoolean(false)
    private var currentPriority = SpeechPriority.BACKGROUND

    private val speechQueue = ConcurrentLinkedQueue<SpeechRequest>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var activeJob: Job? = null

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    fun init(context: Context) {
        if (tts != null) return
        val appContext = context.applicationContext
        audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                try {
                    val hiLocale = Locale("hi", "IN")
                    val result = tts?.setLanguage(hiLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        val enInLocale = Locale("en", "IN")
                        val enResult = tts?.setLanguage(enInLocale)
                        if (enResult == TextToSpeech.LANG_MISSING_DATA || enResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            tts?.language = Locale.getDefault()
                        }
                    }
                    tts?.setSpeechRate(0.96f)
                    tts?.setPitch(1.04f)
                    setupUtteranceListener()
                    Log.i(TAG, "Unified Assistant Voice Engine initialized successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error configuring Assistant voice engine", e)
                }
            } else {
                Log.e(TAG, "Assistant voice engine initialization failed: $status")
            }
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isCurrentlySpeaking.set(true)
            }

            override fun onDone(utteranceId: String?) {
                isCurrentlySpeaking.set(false)
                abandonAudioFocus()
                processNextInQueue()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isCurrentlySpeaking.set(false)
                abandonAudioFocus()
                processNextInQueue()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isCurrentlySpeaking.set(false)
                abandonAudioFocus()
                processNextInQueue()
            }
        })
    }

    /**
     * Primary speech entrypoint. All modules use this method or its typed helpers.
     */
    fun speak(
        text: String,
        priority: SpeechPriority = SpeechPriority.NORMAL,
        onComplete: (() -> Unit)? = null
    ) {
        if (text.isBlank()) return
        Log.d(TAG, "Speech requested [Priority=$priority]: '$text'")

        val request = SpeechRequest(text = text, priority = priority, onComplete = onComplete)

        // If priority is higher than current speech (e.g. Call or Security Warning), preempt
        if (isCurrentlySpeaking.get() && priority.value > currentPriority.value) {
            stopSpeaking()
            playSpeechRequest(request)
        } else if (isCurrentlySpeaking.get()) {
            queueSpeech(request)
        } else {
            playSpeechRequest(request)
        }
    }

    fun speakNotification(sender: String, app: String, message: String) {
        val cleanSender = if (sender.isBlank() || sender.equals("Someone", ignoreCase = true)) "नया संदेश" else sender
        val text = if (message.isBlank()) {
            "$cleanSender से $app पर नया संदेश आया है।"
        } else {
            "$cleanSender से $app पर संदेश: '$message'"
        }
        speak(text, SpeechPriority.NOTIFICATION)
    }

    fun speakCallAnnouncement(caller: String, isWhatsApp: Boolean) {
        val app = if (isWhatsApp) "WhatsApp" else "फ़ोन"
        val text = "$caller का $app कॉल आ रहा है।"
        speak(text, SpeechPriority.CALL_ANNOUNCEMENT)
    }

    fun speakSecurityWarning(message: String) {
        speak(message, SpeechPriority.SECURITY_WARNING)
    }

    fun speakActionResult(action: String, verified: Boolean, message: String) {
        val text = if (verified) message else "काम पूरा नहीं हो पाया: $message"
        speak(text, SpeechPriority.ACTION_RESULT)
    }

    fun queueSpeech(request: SpeechRequest) {
        speechQueue.offer(request)
    }

    fun queueSpeech(text: String, priority: SpeechPriority = SpeechPriority.NORMAL) {
        queueSpeech(SpeechRequest(text = text, priority = priority))
    }

    private fun playSpeechRequest(request: SpeechRequest) {
        activeJob?.cancel()
        currentPriority = request.priority

        activeJob = scope.launch {
            requestAudioFocus()
            isCurrentlySpeaking.set(true)

            if (!isTtsReady || tts == null) {
                // If TTS is still loading, wait up to 1 second
                var waitCount = 0
                while (!isTtsReady && waitCount < 10) {
                    delay(100)
                    waitCount++
                }
            }

            try {
                val params = android.os.Bundle()
                params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                tts?.speak(request.text, TextToSpeech.QUEUE_FLUSH, params, request.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing speech output", e)
                isCurrentlySpeaking.set(false)
                abandonAudioFocus()
            }
        }
    }

    private fun processNextInQueue() {
        val next = speechQueue.poll()
        if (next != null) {
            playSpeechRequest(next)
        } else {
            currentPriority = SpeechPriority.BACKGROUND
        }
    }

    fun stopSpeaking() {
        speechQueue.clear()
        isCurrentlySpeaking.set(false)
        activeJob?.cancel()
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        abandonAudioFocus()
    }

    fun pauseSpeaking() {
        try {
            tts?.stop()
        } catch (e: Exception) {}
        isCurrentlySpeaking.set(false)
    }

    fun resumeSpeaking() {
        processNextInQueue()
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { /* Handle focus loss if needed */ }
                    .build()
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }

    private fun abandonAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus", e)
        }
    }
}
