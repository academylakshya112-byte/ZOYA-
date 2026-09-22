package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

enum class PinState {
    INPUT,
    SUCCESS,
    ERROR
}

@Composable
fun PinLockView(
    modifier: Modifier = Modifier,
    pinLength: Int = 4,
    pinState: PinState = PinState.INPUT,
    onPinCompleted: (String) -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(pinState) {
        when (pinState) {
            PinState.INPUT -> {
                enteredPin = ""
            }
            PinState.ERROR -> {
                coroutineScope.launch {
                    shakeOffset.animateTo(
                        targetValue = 0f,
                        animationSpec = keyframes {
                            durationMillis = 400
                            -20f at 50
                            20f at 100
                            -15f at 150
                            15f at 200
                            -10f at 250
                            10f at 300
                            0f at 400
                        }
                    )
                    enteredPin = ""
                }
            }
            PinState.SUCCESS -> {
                // Keep filled for a moment
            }
        }
    }

    val dotColor by animateColorAsState(
        targetValue = when (pinState) {
            PinState.SUCCESS -> Color(0xFF00E676)
            PinState.ERROR -> Color(0xFFFF5252)
            PinState.INPUT -> Color(0xFF00E5FF)
        },
        label = "pinDotColor"
    )

    Column(
        modifier = modifier.offset(x = shakeOffset.value.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 20.dp)
        ) {
            for (i in 0 until pinLength) {
                val isFilled = i < enteredPin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFilled) dotColor else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isFilled) dotColor else Color(0xFF755B90),
                            shape = CircleShape
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid (1-9, Clear, 0, Backspace)
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "DEL")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    row.forEach { key ->
                        KeypadButton(
                            text = key,
                            onClick = {
                                if (pinState != PinState.INPUT && pinState != PinState.ERROR) return@KeypadButton

                                when (key) {
                                    "C" -> {
                                        enteredPin = ""
                                    }
                                    "DEL" -> {
                                        if (enteredPin.isNotEmpty()) {
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    }
                                    else -> {
                                        if (enteredPin.length < pinLength) {
                                            val newPin = enteredPin + key
                                            enteredPin = newPin
                                            if (newPin.length == pinLength) {
                                                onPinCompleted(newPin)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = Color(0xFF00E5FF)),
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (text == "C" || text == "DEL") Color(0xFF261938) else Color(0xFF1E132D),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (text == "C" || text == "DEL") Color(0xFF452B65) else Color(0xFF382352)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            when (text) {
                "DEL" -> {
                    Icon(
                        Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5277),
                        modifier = Modifier.size(24.dp)
                    )
                }
                "C" -> {
                    Text(
                        text = "CLR",
                        color = Color(0xFFB39DDB),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
