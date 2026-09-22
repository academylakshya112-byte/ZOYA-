package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.MayaLockManager
import com.example.core.MayaLockType
import com.example.ui.components.PatternLockView
import com.example.ui.components.PatternState
import com.example.ui.components.PinLockView
import com.example.ui.components.PinState
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MayaUnlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show when device is locked and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F0818)
                ) {
                    MayaUnlockScreen(
                        onUnlockSuccess = {
                            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                km?.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                                    override fun onDismissSucceeded() {
                                        super.onDismissSucceeded()
                                        finish()
                                    }
                                    override fun onDismissError() {
                                        super.onDismissError()
                                        finish()
                                    }
                                    override fun onDismissCancelled() {
                                        super.onDismissCancelled()
                                        finish()
                                    }
                                })
                            } else {
                                finish()
                            }
                        },
                        onEmergencyDismiss = {
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MayaUnlockScreen(
    onUnlockSuccess: () -> Unit,
    onEmergencyDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lockType = remember { MayaLockManager.getLockType(context) }

    var pinState by remember { mutableStateOf(PinState.INPUT) }
    var patternState by remember { mutableStateOf(PatternState.INPUT) }
    var statusMessage by remember { mutableStateOf("Maya Security: Apne phone ko unlock karein") }
    var isSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF140A22),
                        Color(0xFF090412)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🔒",
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Maya Guarded Lock",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = statusMessage,
                    color = if (isSuccess) Color(0xFF00E676) else if (pinState == PinState.ERROR || patternState == PatternState.ERROR) Color(0xFFFF5252) else Color(0xFFB39DDB),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Credential Input UI
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when (lockType) {
                    MayaLockType.PIN -> {
                        PinLockView(
                            pinLength = 4,
                            pinState = pinState,
                            onPinCompleted = { inputPin ->
                                val ok = MayaLockManager.verifyPin(context, inputPin)
                                if (ok) {
                                    pinState = PinState.SUCCESS
                                    isSuccess = true
                                    statusMessage = "PIN Match! Phone Unlock ho raha hai..."
                                    coroutineScope.launch {
                                        delay(400)
                                        onUnlockSuccess()
                                    }
                                } else {
                                    pinState = PinState.ERROR
                                    statusMessage = "Galat PIN! Kripya dobara enter karein."
                                    coroutineScope.launch {
                                        delay(1200)
                                        pinState = PinState.INPUT
                                        statusMessage = "Maya PIN enter karein"
                                    }
                                }
                            }
                        )
                    }
                    MayaLockType.PATTERN -> {
                        PatternLockView(
                            modifier = Modifier.size(320.dp),
                            patternState = patternState,
                            onPatternCompleted = { patternDots ->
                                val ok = MayaLockManager.verifyPattern(context, patternDots)
                                if (ok) {
                                    patternState = PatternState.SUCCESS
                                    isSuccess = true
                                    statusMessage = "Pattern Match! Phone Unlock ho raha hai..."
                                    coroutineScope.launch {
                                        delay(400)
                                        onUnlockSuccess()
                                    }
                                } else {
                                    patternState = PatternState.ERROR
                                    statusMessage = "Galat Pattern! Dobara koshish karein."
                                    coroutineScope.launch {
                                        delay(1200)
                                        patternState = PatternState.INPUT
                                        statusMessage = "Maya Pattern draw karein"
                                    }
                                }
                            }
                        )
                    }
                    MayaLockType.NONE -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Koi Maya Credential Set Nahi Hai.",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onUnlockSuccess,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Text("Seedhe Unlock Karein", color = Color.Black)
                            }
                        }
                    }
                }
            }

            // Bottom Emergency / Dismiss action
            TextButton(
                onClick = onEmergencyDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "System Lock Screen Par Jayein ↗",
                    color = Color(0xFF80D8FF),
                    fontSize = 13.sp
                )
            }
        }
    }
}
