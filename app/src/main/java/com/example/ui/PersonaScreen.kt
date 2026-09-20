package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ZoyaForegroundService
import com.example.notification.CallAnnouncer
import com.example.persona.PersonaManager
import com.example.persona.PersonaType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedPersona by remember {
        mutableStateOf(PersonaManager.getSelectedPersona(context))
    }

    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    val assistantName = remember { prefs.getString("assistant_name", "MAYA") ?: "MAYA" }
    val bossName = remember { prefs.getString("boss_name", "Boss") ?: "Boss" }

    val personaList = listOf(
        PersonaType.CARING_SWEET,
        PersonaType.PLAYFUL_NAKHRE,
        PersonaType.SUPER_FRIENDLY
    )

    Scaffold(
        containerColor = Color(0xFF0F0A1C),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Persona Settings",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Customize $assistantName's personality & style",
                            color = Color(0xFFB39DDB),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0A1C))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B1228)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF3B2555))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    Color(selectedPersona.primaryColor).copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .border(1.5.dp, Color(selectedPersona.primaryColor), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(selectedPersona.iconEmoji, fontSize = 24.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Mode: ${selectedPersona.displayName}",
                                color = Color(selectedPersona.primaryColor),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Dynamic replies across all chats, voice sessions & actions.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // The 3 Persona Cards Section Header
            item {
                Text(
                    text = "SELECT PERSONALITY MODE",
                    color = Color(0xFFB39DDB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            // Exactly 3 Selectable Persona Cards
            items(personaList) { persona ->
                val isSelected = selectedPersona == persona
                val primaryColor = Color(persona.primaryColor)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(if (isSelected) 10.dp else 2.dp, RoundedCornerShape(20.dp))
                        .clickable {
                            selectedPersona = persona
                            PersonaManager.setSelectedPersona(context, persona)
                            Toast.makeText(
                                context,
                                "${persona.displayName} mode activated!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF221634) else Color(0xFF160E22)
                    ),
                    border = BorderStroke(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) primaryColor else Color(0xFF2F1D46)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        // Card Header: Icon, Name, Tag & Radio Selection State
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .border(1.dp, primaryColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(persona.iconEmoji, fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        text = persona.displayName,
                                        color = if (isSelected) primaryColor else Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = persona.tag,
                                        color = Color(0xFFB39DDB),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Selection Indicator / Radio
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedPersona = persona
                                    PersonaManager.setSelectedPersona(context, persona)
                                    Toast.makeText(
                                        context,
                                        "${persona.displayName} mode activated!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = primaryColor,
                                    unselectedColor = Color(0xFF5E427B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Short Description
                        Text(
                            text = persona.shortDescription,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Sample Conversational Phrases Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Sample conversational vibe:",
                                color = primaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            persona.samplePhrases.take(3).forEach { phrase ->
                                Text(
                                    text = "• $phrase",
                                    color = Color(0xFFE1D5F5),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview Voice / Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val sampleText = when (persona) {
                                        PersonaType.CARING_SWEET -> "Haan $bossName sona, batao kya help kar sakti hoon aapki?"
                                        PersonaType.PLAYFUL_NAKHRE -> "Achhaaa babu, ab yaad aayi meri? Batao kya order hai!"
                                        PersonaType.SUPER_FRIENDLY -> "Arre wah $bossName! Chalo batao kya kaam hai, abhi karte hain!"
                                    }
                                    CallAnnouncer.speakText(sampleText)
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Voice Preview", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Listen Voice Sample", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .background(primaryColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = primaryColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Policy & Respect Note
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "💡 Note: Responses are dynamically generated for fresh conversations. Persona never interferes with WhatsApp messages, calls, or tool reliability.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
