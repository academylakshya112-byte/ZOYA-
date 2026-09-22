package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

enum class PatternState {
    INPUT,
    SUCCESS,
    ERROR
}

@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    patternState: PatternState = PatternState.INPUT,
    onPatternCompleted: (List<Int>) -> Unit
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPoint by remember { mutableStateOf<Offset?>(null) }
    var nodePositions by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Clear nodes on state change to input
    LaunchedEffect(patternState) {
        if (patternState == PatternState.INPUT) {
            selectedNodes.clear()
            currentTouchPoint = null
        }
    }

    val lineColor by animateColorAsState(
        targetValue = when (patternState) {
            PatternState.SUCCESS -> Color(0xFF00E676)
            PatternState.ERROR -> Color(0xFFFF5252)
            PatternState.INPUT -> Color(0xFF00E5FF)
        },
        animationSpec = tween(200),
        label = "patternLineColor"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(patternState) {
                if (patternState != PatternState.INPUT) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        selectedNodes.clear()
                        currentTouchPoint = offset
                        nodePositions.forEachIndexed { index, nodeOffset ->
                            val dist = sqrt((offset.x - nodeOffset.x).pow(2) + (offset.y - nodeOffset.y).pow(2))
                            if (dist < 80f && !selectedNodes.contains(index)) {
                                selectedNodes.add(index)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPoint = change.position
                        nodePositions.forEachIndexed { index, nodeOffset ->
                            val dist = sqrt(
                                (change.position.x - nodeOffset.x).pow(2) +
                                        (change.position.y - nodeOffset.y).pow(2)
                            )
                            if (dist < 80f && !selectedNodes.contains(index)) {
                                selectedNodes.add(index)
                            }
                        }
                    },
                    onDragEnd = {
                        currentTouchPoint = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternCompleted(selectedNodes.toList())
                        }
                    },
                    onDragCancel = {
                        currentTouchPoint = null
                        selectedNodes.clear()
                    }
                )
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Calculate positions for 3x3 grid
        val stepX = width / 4f
        val stepY = height / 4f
        val calculatedPositions = remember(width, height) {
            val list = mutableListOf<Offset>()
            for (row in 1..3) {
                for (col in 1..3) {
                    list.add(Offset(col * stepX, row * stepY))
                }
            }
            list
        }
        nodePositions = calculatedPositions

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw connecting lines between selected nodes
            if (selectedNodes.size > 1) {
                for (i in 0 until selectedNodes.size - 1) {
                    val start = calculatedPositions[selectedNodes[i]]
                    val end = calculatedPositions[selectedNodes[i + 1]]
                    drawLine(
                        color = lineColor,
                        start = start,
                        end = end,
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line to current touch finger position
            if (selectedNodes.isNotEmpty() && currentTouchPoint != null && patternState == PatternState.INPUT) {
                val lastNode = calculatedPositions[selectedNodes.last()]
                drawLine(
                    color = lineColor.copy(alpha = 0.7f),
                    start = lastNode,
                    end = currentTouchPoint!!,
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }

            // Draw nodes
            calculatedPositions.forEachIndexed { index, center ->
                val isSelected = selectedNodes.contains(index)

                // Outer circle glow if selected
                if (isSelected) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.25f),
                        radius = 42f,
                        center = center
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 22f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 10f,
                        center = center
                    )
                } else {
                    drawCircle(
                        color = Color(0xFF382555),
                        radius = 28f,
                        center = center
                    )
                    drawCircle(
                        color = Color(0xFFB39DDB),
                        radius = 10f,
                        center = center
                    )
                }
            }
        }
    }
}
