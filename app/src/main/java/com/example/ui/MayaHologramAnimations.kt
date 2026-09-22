package com.example.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.live.ZoyaState
import kotlin.math.cos
import kotlin.math.sin

/**
 * 6 Ultra-Premium High-Tech Futuristic Hologram Animation Types for M.A.Y.A
 */
enum class MayaAnimationType(
    val id: String,
    val displayName: String,
    val tag: String,
    val subtitle: String,
    val iconEmoji: String,
    val accentColor: Long
) {
    CYBER_HUD(
        id = "cyber_hud",
        displayName = "Cyber HUD M.A.Y.A",
        tag = "ORIGINAL HUD",
        subtitle = "Neon Cyan & Magenta rotating arcs with dynamic soundwave frequency dial",
        iconEmoji = "🎯",
        accentColor = 0xFF00E5FF
    ),
    JARVIS_ARC(
        id = "jarvis_arc",
        displayName = "J.A.R.V.I.S Arc Core",
        tag = "STARK TECH",
        subtitle = "Iron Man holographic arc reactor with dual counter-rotating telemetry dials",
        iconEmoji = "⚡",
        accentColor = 0xFF00B0FF
    ),
    NEON_VORTEX(
        id = "neon_vortex",
        displayName = "Neon Cyber Vortex",
        tag = "SINGULARITY",
        subtitle = "Multi-spiral holographic vortex with orbiting quantum satellites & AI core",
        iconEmoji = "🌌",
        accentColor = 0xFFD500F9
    ),
    CYBER_SENTINEL(
        id = "cyber_sentinel",
        displayName = "Cyber Sentinel Matrix",
        tag = "DEFENSE HUD",
        subtitle = "Hexagonal cyber energy shield with rotating lock segments & radar sweep",
        iconEmoji = "🛡️",
        accentColor = 0xFF00E676
    ),
    QUANTUM_NEXUS(
        id = "quantum_nexus",
        displayName = "Quantum Neural Nexus",
        tag = "QUANTUM AI",
        subtitle = "Gyroscopic quantum processor with laser synapse qubits & prism lens",
        iconEmoji = "🧬",
        accentColor = 0xFFFFD54F
    ),
    SOLAR_SUPERNOVA(
        id = "solar_supernova",
        displayName = "Solar Plasma Supernova",
        tag = "PLASMA CORE",
        subtitle = "High-energy solar flare plasma HUD with magnetic coronal loops & tachometer dial",
        iconEmoji = "🔥",
        accentColor = 0xFFFF3D00
    );

    companion object {
        fun fromId(id: String?): MayaAnimationType {
            return entries.firstOrNull { it.id == id } ?: CYBER_HUD
        }
    }
}

object MayaAnimationManager {
    private const val PREFS_KEY = "maya_animation_theme"

    fun getSelectedAnimation(context: Context): MayaAnimationType {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        val id = prefs.getString(PREFS_KEY, MayaAnimationType.CYBER_HUD.id)
        return MayaAnimationType.fromId(id)
    }

    fun setSelectedAnimation(context: Context, type: MayaAnimationType) {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_KEY, type.id).apply()
    }
}

/**
 * Master Dynamic Hologram Renderer
 */
@Composable
fun MayaMasterAnimation(
    state: ZoyaState,
    animationType: MayaAnimationType = MayaAnimationType.CYBER_HUD,
    assistantName: String = "M.A.Y.A",
    size: Dp = 280.dp,
    onAnimationClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .size(size)
            .then(if (onAnimationClick != null) Modifier.clickable { onAnimationClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when (animationType) {
            MayaAnimationType.CYBER_HUD -> CyberHudAnimation(state = state, assistantName = assistantName, size = size)
            MayaAnimationType.JARVIS_ARC -> JarvisArcAnimation(state = state, assistantName = assistantName, size = size)
            MayaAnimationType.NEON_VORTEX -> NeonVortexAnimation(state = state, assistantName = assistantName, size = size)
            MayaAnimationType.CYBER_SENTINEL -> CyberSentinelAnimation(state = state, assistantName = assistantName, size = size)
            MayaAnimationType.QUANTUM_NEXUS -> QuantumNexusAnimation(state = state, assistantName = assistantName, size = size)
            MayaAnimationType.SOLAR_SUPERNOVA -> SolarSupernovaAnimation(state = state, assistantName = assistantName, size = size)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. DEFAULT: CYBER HUD (M.A.Y.A) - Exactly as shown in User's Reference Image
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberHudAnimation(
    state: ZoyaState,
    assistantName: String = "M.A.Y.A",
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyberHud")

    val cyanArcRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "cyanArc"
    )

    val whiteArcRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "whiteArc"
    )

    val magentaPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "magentaPulse"
    )

    val scaleFactor = size.value / 280f

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.minDimension / 2.15f

            // Tech Circuit Tracks & Corner Crosshairs (+)
            drawTechCircuitGrid(center, outerRadius)

            // Outer Thin HUD Dial with tick marks
            drawCircle(
                color = Color(0xFF1E2A4A).copy(alpha = 0.7f),
                radius = outerRadius * 0.98f,
                style = Stroke(width = 1.5f * scaleFactor)
            )

            val tickCount = 24
            for (i in 0 until tickCount) {
                val angle = (i * 360f / tickCount) * (Math.PI.toFloat() / 180f)
                val r1 = outerRadius * 0.95f
                val r2 = outerRadius * 0.99f
                drawLine(
                    color = if (i % 6 == 0) Color(0xFF00E5FF) else Color(0xFF384B70),
                    start = Offset(center.x + r1 * cos(angle), center.y + r1 * sin(angle)),
                    end = Offset(center.x + r2 * cos(angle), center.y + r2 * sin(angle)),
                    strokeWidth = (if (i % 6 == 0) 2.5f else 1.5f) * scaleFactor
                )
            }

            // Middle Magenta / Purple Neon Arc
            rotate(degrees = -cyanArcRotation * 0.5f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFFFF007F), Color(0xFFD500F9), Color(0xFFFF007F))
                    ),
                    startAngle = 190f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius * 0.88f, center.y - outerRadius * 0.88f),
                    size = Size(outerRadius * 1.76f, outerRadius * 1.76f),
                    style = Stroke(width = 5f * magentaPulse * scaleFactor, cap = StrokeCap.Round)
                )
            }

            // Prominent Cyan Glowing Arc (Top Left)
            rotate(degrees = cyanArcRotation, pivot = center) {
                drawArc(
                    color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                    startAngle = 135f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius * 0.82f, center.y - outerRadius * 0.82f),
                    size = Size(outerRadius * 1.64f, outerRadius * 1.64f),
                    style = Stroke(width = 16f * scaleFactor, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFF80D8FF), Color(0xFF00E5FF))
                    ),
                    startAngle = 135f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius * 0.82f, center.y - outerRadius * 0.82f),
                    size = Size(outerRadius * 1.64f, outerRadius * 1.64f),
                    style = Stroke(width = 9f * scaleFactor, cap = StrokeCap.Round)
                )

                val startRad = 135f * (Math.PI.toFloat() / 180f)
                drawCircle(
                    color = Color.White,
                    radius = 5.5f * scaleFactor,
                    center = Offset(center.x + outerRadius * 0.82f * cos(startRad), center.y + outerRadius * 0.82f * sin(startRad))
                )
            }

            // Heavy White / Bright Cyan Glowing Arc (Bottom Right)
            rotate(degrees = whiteArcRotation, pivot = center) {
                drawArc(
                    color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                    startAngle = 320f,
                    sweepAngle = 115f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius * 0.82f, center.y - outerRadius * 0.82f),
                    size = Size(outerRadius * 1.64f, outerRadius * 1.64f),
                    style = Stroke(width = 20f * scaleFactor, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF80D8FF), Color.White, Color(0xFF00E5FF))
                    ),
                    startAngle = 320f,
                    sweepAngle = 115f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerRadius * 0.82f, center.y - outerRadius * 0.82f),
                    size = Size(outerRadius * 1.64f, outerRadius * 1.64f),
                    style = Stroke(width = 11f * scaleFactor, cap = StrokeCap.Round)
                )

                val tipRad = 320f * (Math.PI.toFloat() / 180f)
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 6f * scaleFactor,
                    center = Offset(center.x + outerRadius * 0.82f * cos(tipRad), center.y + outerRadius * 0.82f * sin(tipRad))
                )
            }

            // Central Deep Cyber HUD Sphere Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2A163B).copy(alpha = 0.95f),
                        Color(0xFF160D25).copy(alpha = 0.98f),
                        Color(0xFF0A0514)
                    ),
                    center = center,
                    radius = outerRadius * 0.65f
                ),
                radius = outerRadius * 0.65f
            )

            drawCircle(
                color = Color(0xFF00E5FF).copy(alpha = 0.3f),
                radius = outerRadius * 0.64f,
                style = Stroke(width = 1.5f * scaleFactor)
            )
        }

        // Pure Holographic Visual Core (No Name or Text)
        Box(
            modifier = Modifier.size((120 * scaleFactor).dp),
            contentAlignment = Alignment.Center
        ) {
            // Dynamic Audio Equalizer Dots & Bars
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((3 * scaleFactor).dp)
            ) {
                val barCount = 19
                for (i in 0 until barCount) {
                    val distanceFromCenter = kotlin.math.abs(i - barCount / 2)
                    val baseHeight = when {
                        distanceFromCenter > 6 -> 4f
                        distanceFromCenter > 3 -> 12f
                        else -> 24f
                    }

                    val animHeight by infiniteTransition.animateFloat(
                        initialValue = baseHeight * 0.5f,
                        targetValue = baseHeight * (if (state == ZoyaState.SPEAKING || state == ZoyaState.LISTENING) 2.0f else 1.2f),
                        animationSpec = infiniteRepeatable(
                            animation = tween(160 + (i * 30), easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "eq_$i"
                    )

                    val isDot = distanceFromCenter > 7
                    if (isDot) {
                        Box(
                            modifier = Modifier
                                .size((3 * scaleFactor).dp)
                                .clip(CircleShape)
                                .background(Color(0xFF80D8FF).copy(alpha = 0.7f))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width((2.8 * scaleFactor).dp)
                                .height((animHeight * scaleFactor).dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFF4081),
                                            Color(0xFF00E5FF),
                                            Color(0xFF00E676)
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. J.A.R.V.I.S / STARK ARC CORE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun JarvisArcAnimation(
    state: ZoyaState,
    assistantName: String = "J.A.R.V.I.S",
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvisArc")

    val outerDialRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6500, easing = LinearEasing)),
        label = "outerDial"
    )

    val innerDialRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "innerDial"
    )

    val reactorPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "reactorPulse"
    )

    val primaryColor = when (state) {
        ZoyaState.SPEAKING -> Color(0xFF00E5FF)
        ZoyaState.LISTENING -> Color(0xFFFFD54F)
        ZoyaState.THINKING -> Color(0xFFFF9100)
        else -> Color(0xFF00B0FF)
    }

    val scaleFactor = size.value / 280f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2.2f

            // Outer Casing Hexagon Border
            drawCircle(Color(0xFF0D1B2A), radius = radius, style = Stroke(width = 6f * scaleFactor))
            drawCircle(primaryColor.copy(alpha = 0.3f), radius = radius * 0.97f, style = Stroke(width = 1.5f * scaleFactor))

            // Outer Counter-Clockwise Degree Dial
            rotate(degrees = outerDialRotation, pivot = center) {
                val segCount = 36
                for (i in 0 until segCount) {
                    val angle = (i * 360f / segCount) * (Math.PI.toFloat() / 180f)
                    val r1 = radius * 0.88f
                    val r2 = if (i % 6 == 0) radius * 0.96f else radius * 0.92f
                    drawLine(
                        color = if (i % 6 == 0) primaryColor else primaryColor.copy(alpha = 0.4f),
                        start = Offset(center.x + r1 * cos(angle), center.y + r1 * sin(angle)),
                        end = Offset(center.x + r2 * cos(angle), center.y + r2 * sin(angle)),
                        strokeWidth = (if (i % 6 == 0) 3f else 1.5f) * scaleFactor
                    )
                }

                // 4 Quadrant Target Corner Brackets
                val bracketAngles = listOf(0f, 90f, 180f, 270f)
                bracketAngles.forEach { deg ->
                    drawArc(
                        color = Color(0xFFFFAB00),
                        startAngle = deg + 10f,
                        sweepAngle = 25f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 0.88f, center.y - radius * 0.88f),
                        size = Size(radius * 1.76f, radius * 1.76f),
                        style = Stroke(width = 4f * scaleFactor, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner Rotating Electromagnetic Copper Coils & Telemetry Vanes
            rotate(degrees = innerDialRotation, pivot = center) {
                val coilCount = 8
                for (i in 0 until coilCount) {
                    val angle = (i * 360f / coilCount) * (Math.PI.toFloat() / 180f)
                    val p1 = Offset(center.x + radius * 0.78f * cos(angle), center.y + radius * 0.78f * sin(angle))
                    val p2 = Offset(center.x + radius * 0.52f * cos(angle), center.y + radius * 0.52f * sin(angle))
                    drawLine(Color(0xFFFF9100), p1, p2, strokeWidth = 3f * scaleFactor, cap = StrokeCap.Round)
                    drawCircle(primaryColor, radius = 3.5f * scaleFactor, center = p1)
                }
            }

            // Central Pure Arc Plasma Glow
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color.White,
                        primaryColor.copy(alpha = 0.9f),
                        Color(0xFF003060),
                        Color(0xFF050E18)
                    ),
                    center = center,
                    radius = radius * 0.52f * reactorPulse
                ),
                radius = radius * 0.52f * reactorPulse
            )

            // Central Precision Tech Triangle
            val triPath = Path().apply {
                val tr = radius * 0.35f
                for (i in 0 until 3) {
                    val a = (i * 120f - 90f) * (Math.PI.toFloat() / 180f)
                    val x = center.x + tr * cos(a)
                    val y = center.y + tr * sin(a)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(triPath, primaryColor, style = Stroke(width = 2.5f * scaleFactor))
            drawCircle(Color.White, radius = 5f * scaleFactor, center = center)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. NEON CYBER VORTEX (SINGULARITY)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun NeonVortexAnimation(
    state: ZoyaState,
    assistantName: String = "VORTEX",
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neonVortex")

    val spiralRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7500, easing = LinearEasing)),
        label = "spiral"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "counterSpiral"
    )

    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "corePulse"
    )

    val primaryColor = when (state) {
        ZoyaState.SPEAKING -> Color(0xFF00E5FF)
        ZoyaState.LISTENING -> Color(0xFFFF007F)
        ZoyaState.THINKING -> Color(0xFFFFD54F)
        else -> Color(0xFFD500F9)
    }

    val scaleFactor = size.value / 280f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2.2f

            // Outer Cyber Ring
            drawCircle(
                brush = Brush.sweepGradient(listOf(Color(0xFFD500F9), Color(0xFF00E5FF), Color(0xFFFF007F), Color(0xFFD500F9))),
                radius = radius * 0.98f,
                style = Stroke(width = 2f * scaleFactor)
            )

            // 4 Multi-Spiral Galaxy Vortex Arms
            rotate(degrees = spiralRotation, pivot = center) {
                for (arm in 0 until 4) {
                    val baseAngle = arm * 90f
                    val path = Path()
                    for (step in 0..20) {
                        val frac = step / 20f
                        val r = (radius * 0.25f) + (radius * 0.7f * frac)
                        val a = (baseAngle + (frac * 160f)) * (Math.PI.toFloat() / 180f)
                        val x = center.x + r * cos(a)
                        val y = center.y + r * sin(a)
                        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            listOf(Color.White, primaryColor, Color.Transparent)
                        ),
                        style = Stroke(width = (4f * (1f - (arm % 2 * 0.3f))) * scaleFactor, cap = StrokeCap.Round)
                    )
                }
            }

            // Counter Rotating Star Particles
            rotate(degrees = counterRotation, pivot = center) {
                val nodeCount = 12
                for (i in 0 until nodeCount) {
                    val angle = (i * 360f / nodeCount) * (Math.PI.toFloat() / 180f)
                    val r = radius * (0.45f + (i % 3) * 0.22f)
                    val nodePos = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
                    drawCircle(Color.White, radius = (3f + (i % 2) * 2f) * scaleFactor, center = nodePos)
                }
            }

            // Deep Singularity Core
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, primaryColor, Color(0xFF240046), Color(0xFF0A0014)),
                    center = center,
                    radius = radius * 0.42f * corePulse
                ),
                radius = radius * 0.42f * corePulse
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. CYBER SENTINEL MATRIX (DEFENSE HUD)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun CyberSentinelAnimation(
    state: ZoyaState,
    assistantName: String = "SENTINEL",
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cyberSentinel")

    val radarSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "radarSweep"
    )

    val hexRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "hexRot"
    )

    val shieldPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shieldPulse"
    )

    val primaryColor = when (state) {
        ZoyaState.SPEAKING -> Color(0xFF00E676)
        ZoyaState.LISTENING -> Color(0xFF00E5FF)
        ZoyaState.THINKING -> Color(0xFFFFD54F)
        else -> Color(0xFF00E676)
    }

    val scaleFactor = size.value / 280f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2.2f

            // Outer Cyber Hex Shield
            rotate(degrees = hexRotation, pivot = center) {
                val hexPath = Path()
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (Math.PI.toFloat() / 180f)
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)
                    if (i == 0) hexPath.moveTo(x, y) else hexPath.lineTo(x, y)
                }
                hexPath.close()
                drawPath(hexPath, primaryColor.copy(alpha = 0.5f), style = Stroke(width = 2.5f * scaleFactor))

                // Hex Vertex Locks
                for (i in 0 until 6) {
                    val angle = (i * 60f) * (Math.PI.toFloat() / 180f)
                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)
                    drawCircle(primaryColor, radius = 5f * scaleFactor, center = Offset(x, y))
                }
            }

            // Rotating 360 Radar Sweep Beam
            rotate(degrees = radarSweep, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.Transparent, primaryColor.copy(alpha = 0.45f))
                    ),
                    startAngle = 0f,
                    sweepAngle = 75f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius * 0.85f, center.y - radius * 0.85f),
                    size = Size(radius * 1.7f, radius * 1.7f)
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(center.x + radius * 0.85f, center.y),
                    strokeWidth = 2f * scaleFactor
                )
            }

            // Middle Concentric Ring
            drawCircle(primaryColor.copy(alpha = 0.3f), radius = radius * 0.6f, style = Stroke(width = 1.5f * scaleFactor))

            // Central Shield Matrix Orb
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, primaryColor, Color(0xFF003314), Color(0xFF001107)),
                    center = center,
                    radius = radius * 0.45f * shieldPulse
                ),
                radius = radius * 0.45f * shieldPulse
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. QUANTUM NEURAL NEXUS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun QuantumNexusAnimation(
    state: ZoyaState,
    assistantName: String = "QUANTUM",
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "quantumNexus")

    val ring1Rot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "ring1"
    )

    val ring2Rot by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing)),
        label = "ring2"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "pulsePhase"
    )

    val primaryColor = when (state) {
        ZoyaState.SPEAKING -> Color(0xFF00E676)
        ZoyaState.LISTENING -> Color(0xFF00E5FF)
        ZoyaState.THINKING -> Color(0xFFFF9100)
        else -> Color(0xFFFFD54F)
    }

    val scaleFactor = size.value / 280f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2.2f

            // Outer Gyro Ring 1
            rotate(degrees = ring1Rot, pivot = center) {
                drawOval(
                    brush = Brush.sweepGradient(listOf(primaryColor, Color.White, Color.Transparent, primaryColor)),
                    topLeft = Offset(center.x - radius * 0.95f, center.y - radius * 0.45f),
                    size = Size(radius * 1.9f, radius * 0.9f),
                    style = Stroke(width = 3f * scaleFactor)
                )
                drawCircle(Color.White, radius = 5f * scaleFactor, center = Offset(center.x + radius * 0.95f, center.y))
            }

            // Inner Gyro Ring 2 (Tilted)
            rotate(degrees = ring2Rot + 60f, pivot = center) {
                drawOval(
                    brush = Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color.White, Color.Transparent, Color(0xFF00E5FF))),
                    topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.38f),
                    size = Size(radius * 1.64f, radius * 0.76f),
                    style = Stroke(width = 2.5f * scaleFactor)
                )
                drawCircle(Color(0xFF00E5FF), radius = 4.5f * scaleFactor, center = Offset(center.x - radius * 0.82f, center.y))
            }

            // Synapse Laser Nodes
            val nodeCount = 8
            for (i in 0 until nodeCount) {
                val a = (i * 360f / nodeCount) * (Math.PI.toFloat() / 180f)
                val nodePos = Offset(center.x + radius * 0.65f * cos(a), center.y + radius * 0.65f * sin(a))
                drawLine(primaryColor.copy(alpha = 0.35f), center, nodePos, strokeWidth = 1.5f * scaleFactor)

                // Laser pulse particle traveling outward
                val travel = Offset(
                    center.x + (nodePos.x - center.x) * pulsePhase,
                    center.y + (nodePos.y - center.y) * pulsePhase
                )
                drawCircle(Color.White, radius = 3.5f * scaleFactor, center = travel)
            }

            // Quantum AI Core
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White, primaryColor, Color(0xFF332000), Color(0xFF0A0700)),
                    center = center,
                    radius = radius * 0.4f
                ),
                radius = radius * 0.4f
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. SOLAR PLASMA SUPERNOVA
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SolarSupernovaAnimation(
    state: ZoyaState,
    assistantName: String = "SOLAR",
    size: Dp = 280.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "solarSupernova")

    val coronalRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "coronal"
    )

    val dialRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "dial"
    )

    val plasmaFlare by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "plasmaFlare"
    )

    val primaryColor = when (state) {
        ZoyaState.SPEAKING -> Color(0xFFFFD54F)
        ZoyaState.LISTENING -> Color(0xFF00E5FF)
        ZoyaState.THINKING -> Color(0xFFFFAB00)
        else -> Color(0xFFFF3D00)
    }

    val scaleFactor = size.value / 280f

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2.2f

            // Exterior Circular Tachometer Ring with Chevron Arrows
            rotate(degrees = dialRotation, pivot = center) {
                drawCircle(Color(0xFF330A00), radius = radius * 0.98f, style = Stroke(width = 4f * scaleFactor))
                val tickCount = 20
                for (i in 0 until tickCount) {
                    val angle = (i * 360f / tickCount) * (Math.PI.toFloat() / 180f)
                    val r1 = radius * 0.88f
                    val r2 = radius * 0.97f
                    drawLine(
                        color = if (i % 5 == 0) Color(0xFFFFD54F) else Color(0xFFFF3D00).copy(alpha = 0.4f),
                        start = Offset(center.x + r1 * cos(angle), center.y + r1 * sin(angle)),
                        end = Offset(center.x + r2 * cos(angle), center.y + r2 * sin(angle)),
                        strokeWidth = (if (i % 5 == 0) 3.5f else 1.5f) * scaleFactor
                    )
                }
            }

            // Magnetic Coronal Flare Arcs
            rotate(degrees = coronalRotation, pivot = center) {
                for (a in 0 until 3) {
                    val angleOffset = a * 120f
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color(0xFFFF3D00), Color(0xFFFFD54F), Color.Transparent)),
                        startAngle = angleOffset,
                        sweepAngle = 80f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 0.82f, center.y - radius * 0.82f),
                        size = Size(radius * 1.64f, radius * 1.64f),
                        style = Stroke(width = 5f * scaleFactor, cap = StrokeCap.Round)
                    )
                }
            }

            // Pulsating Fiery Fusion Core
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color.White,
                        Color(0xFFFFD54F),
                        Color(0xFFFF3D00),
                        Color(0xFF660000),
                        Color(0xFF1F0000)
                    ),
                    center = center,
                    radius = radius * 0.5f * plasmaFlare
                ),
                radius = radius * 0.5f * plasmaFlare
            )
        }
    }
}

private fun DrawScope.drawTechCircuitGrid(center: Offset, radius: Float) {
    val crosshairColor = Color(0xFF00E5FF).copy(alpha = 0.6f)
    val gridLineColor = Color(0xFF293B61).copy(alpha = 0.5f)

    val crossOffset = radius * 0.92f
    val crossPositions = listOf(
        Offset(center.x - crossOffset * 0.45f, center.y - crossOffset * 0.95f),
        Offset(center.x + crossOffset * 0.45f, center.y - crossOffset * 0.95f),
        Offset(center.x - crossOffset * 0.85f, center.y + crossOffset * 0.75f),
        Offset(center.x + crossOffset * 0.85f, center.y + crossOffset * 0.75f),
        Offset(center.x + crossOffset * 0.95f, center.y - crossOffset * 0.25f),
        Offset(center.x - crossOffset * 0.95f, center.y - crossOffset * 0.25f)
    )

    crossPositions.forEach { pos ->
        val arm = 9f
        drawLine(crosshairColor, Offset(pos.x - arm, pos.y), Offset(pos.x + arm, pos.y), strokeWidth = 2f)
        drawLine(crosshairColor, Offset(pos.x, pos.y - arm), Offset(pos.x, pos.y + arm), strokeWidth = 2f)
    }

    val pathLeft = Path().apply {
        moveTo(center.x - radius * 1.05f, center.y - radius * 0.35f)
        lineTo(center.x - radius * 0.85f, center.y - radius * 0.35f)
        lineTo(center.x - radius * 0.75f, center.y - radius * 0.55f)
        lineTo(center.x - radius * 0.65f, center.y - radius * 0.55f)
    }
    drawPath(pathLeft, gridLineColor, style = Stroke(width = 1.5f))

    val pathRight = Path().apply {
        moveTo(center.x + radius * 1.05f, center.y + radius * 0.35f)
        lineTo(center.x + radius * 0.85f, center.y + radius * 0.35f)
        lineTo(center.x + radius * 0.75f, center.y + radius * 0.55f)
        lineTo(center.x + radius * 0.65f, center.y + radius * 0.55f)
    }
    drawPath(pathRight, gridLineColor, style = Stroke(width = 1.5f))

    drawCircle(Color(0xFFFF4081), radius = 3f, center = Offset(center.x - radius * 1.05f, center.y - radius * 0.35f))
    drawCircle(Color(0xFFFF4081), radius = 3f, center = Offset(center.x + radius * 1.05f, center.y + radius * 0.35f))
}
