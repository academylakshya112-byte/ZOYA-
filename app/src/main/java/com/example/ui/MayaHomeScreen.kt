package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayaHomeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedAnim by remember {
        mutableStateOf(MayaAnimationManager.getSelectedAnimation(context))
    }

    // Live or simulated state for testing visual responsiveness
    var previewState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var overrideTestState by remember { mutableStateOf<ZoyaState?>(null) }

    val effectiveState = overrideTestState ?: previewState

    val assistantName = remember {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        prefs.getString("assistant_name", "MAYA") ?: "MAYA"
    }

    Scaffold(
        containerColor = Color(0xFF0D0714),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Maya Home",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "🎯", fontSize = 16.sp)
                        }
                        Text(
                            text = "Live 3D Hologram Gallery • 6 Themes",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF160D20)
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF160D20),
                            Color(0xFF0D0714),
                            Color(0xFF05020A)
                        )
                    )
                ),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────────────────────────────────────────
            // 1. TOP ACTIVE HERO SHOWCASE
            // ─────────────────────────────────────────────
            item(span = { GridItemSpan(1) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(selectedAnim.accentColor)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF191024)),
                    border = BorderStroke(1.5.dp, Color(selectedAnim.accentColor).copy(alpha = 0.8f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE HOLOGRAM ON HOME",
                                    color = Color(0xFF00E676),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = selectedAnim.tag,
                                color = Color(selectedAnim.accentColor),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(Color(selectedAnim.accentColor).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Live Showcase Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            MayaMasterAnimation(
                                state = effectiveState,
                                animationType = selectedAnim,
                                assistantName = assistantName,
                                size = 250.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = selectedAnim.displayName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test Mode State Chips (Interactive Sandbox preview)
                        Text(
                            text = "Test Reactivity States:",
                            color = Color(0xFF9E8EBA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                null to "Auto Live",
                                ZoyaState.LISTENING to "🎧 Listening",
                                ZoyaState.SPEAKING to "🗣️ Speaking",
                                ZoyaState.THINKING to "⚡ Processing"
                            ).forEach { (st, label) ->
                                val isChosen = overrideTestState == st
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isChosen) Color(selectedAnim.accentColor).copy(alpha = 0.25f)
                                            else Color(0xFF241733)
                                        )
                                        .border(
                                            1.dp,
                                            if (isChosen) Color(selectedAnim.accentColor) else Color(0xFF38234D),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { overrideTestState = st }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isChosen) Color.White else Color(0xFFB0A5C4),
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 2. HEADER: ALL 6 LIVE HOLOGRAM ANIMATIONS
            // ─────────────────────────────────────────────
            item(span = { GridItemSpan(1) }) {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                    Text(
                        text = "Choose Your Hologram (6 Live Themes)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any live animated card below to set it as your Home assistant avatar.",
                        color = Color(0xFFB0A5C4),
                        fontSize = 12.sp
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 3. 6 LIVE ANIMATION CARDS (REAL-TIME CANVAS)
            // ─────────────────────────────────────────────
            items(MayaAnimationType.entries) { anim ->
                val isSelected = selectedAnim == anim
                val animColor = Color(anim.accentColor)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (isSelected) 14.dp else 4.dp, RoundedCornerShape(20.dp), spotColor = animColor)
                        .clickable {
                            selectedAnim = anim
                            MayaAnimationManager.setSelectedAnimation(context, anim)
                            Toast.makeText(context, "${anim.displayName} Activated!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF211432) else Color(0xFF170D24)
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) animColor else Color(0xFF38234D)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Row: Tag, Name & Selection Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(anim.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = anim.displayName,
                                            color = if (isSelected) Color.White else Color(0xFFE1D5F5),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (anim == MayaAnimationType.CYBER_HUD) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "DEFAULT",
                                                color = Color(0xFF00E5FF),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = anim.tag,
                                        color = animColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            if (isSelected) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(animColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .border(1.dp, animColor, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = animColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ACTIVE",
                                        color = animColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 🌟 LIVE REAL-TIME ANIMATED CANVAS CONTAINER
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF0B0512))
                                .border(1.dp, Color(0xFF261836), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            MayaMasterAnimation(
                                state = effectiveState,
                                animationType = anim,
                                assistantName = assistantName,
                                size = 200.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Button
                        Button(
                            onClick = {
                                selectedAnim = anim
                                MayaAnimationManager.setSelectedAnimation(context, anim)
                                Toast.makeText(context, "${anim.displayName} Activated!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) animColor else Color(0xFF2C1940)
                            ),
                            border = if (!isSelected) BorderStroke(1.dp, Color(0xFF4C2F6B)) else null
                        ) {
                            Text(
                                text = if (isSelected) "✓ Currently Active Hologram" else "Activate This Hologram",
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
