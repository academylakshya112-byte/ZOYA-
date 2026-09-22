package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SosContact
import com.example.model.SosContactsManager
import com.example.notification.CallAnnouncer
import com.example.notification.NotificationManagerHelper
import com.example.persona.PersonaManager
import com.example.persona.PersonaType
import com.example.service.FloatingOrbService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAdvanced: () -> Unit = {},
    onNavigateToPersona: () -> Unit = {},
    onNavigateToLock: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }

    // Persona state
    var selectedPersona by remember {
        mutableStateOf(PersonaManager.getSelectedPersona(context))
    }

    // Animation Theme State (6 Futuristic Animations)
    var selectedAnimation by remember {
        mutableStateOf(MayaAnimationManager.getSelectedAnimation(context))
    }

    // Core Profile states
    var yourName by remember { mutableStateOf(prefs.getString("boss_name", "shadow x rahul") ?: "shadow x rahul") }
    var assistantName by remember { mutableStateOf(prefs.getString("assistant_name", "MAYA") ?: "MAYA") }

    // Music states
    var musicApp by remember { mutableStateOf(prefs.getString("preferred_music_app", "YT Music") ?: "YT Music") }
    var favoriteSong by remember { mutableStateOf(prefs.getString("favorite_song", "") ?: "") }

    // Language & Country code states
    var selectedLanguage by remember {
        mutableStateOf(prefs.getString("app_language", "Hinglish (Hindi + English) — default") ?: "Hinglish (Hindi + English) — default")
    }
    var languageExpanded by remember { mutableStateOf(false) }
    val languageOptions = listOf(
        "Hinglish (Hindi + English) — default",
        "Bhojpuri (भोजपुरी)",
        "Hindi (हिन्दी)",
        "English (India)",
        "English (US)"
    )

    var selectedCountryCode by remember {
        mutableStateOf(prefs.getString("country_code", "🇮🇳 India (+91)") ?: "🇮🇳 India (+91)")
    }
    var countryExpanded by remember { mutableStateOf(false) }
    val countryOptions = listOf(
        "🇮🇳 India (+91)",
        "🇺🇸 United States (+1)",
        "🇬🇧 United Kingdom (+44)",
        "🇦🇪 UAE (+971)",
        "🇨🇦 Canada (+1)",
        "🇦🇺 Australia (+61)"
    )

    // SOS Contacts
    var sosContacts by remember { mutableStateOf(SosContactsManager.getSosContacts(context)) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }

    // Toggles (Screenshot 3)
    var echoGuard by remember { mutableStateOf(prefs.getBoolean("echo_guard", true)) }
    var proactiveMaya by remember { mutableStateOf(prefs.getBoolean("proactive_maya", true)) }
    var floatingOrb by remember { mutableStateOf(prefs.getBoolean("floating_orb", false)) }
    var startOnBoot by remember { mutableStateOf(prefs.getBoolean("start_on_boot", true)) }

    // Call & Message announcements
    var callAnnouncementEnabled by remember { mutableStateOf(CallAnnouncer.isCallAnnouncementEnabled(context)) }
    var phoneCallEnabled by remember { mutableStateOf(CallAnnouncer.isPhoneCallAnnouncementEnabled(context)) }
    var whatsappCallEnabled by remember { mutableStateOf(CallAnnouncer.isWhatsAppCallAnnouncementEnabled(context)) }
    var whatsappMsgEnabled by remember { mutableStateOf(CallAnnouncer.isWhatsAppMessageAnnouncementEnabled(context)) }
    var smsMsgEnabled by remember { mutableStateOf(CallAnnouncer.isSmsMessageAnnouncementEnabled(context)) }
    var repeatAnnouncement by remember { mutableStateOf(CallAnnouncer.isRepeatEnabled(context)) }

    val pinkAccent = Color(0xFFFF5277)
    val cardBg = Color(0xFF1B1226)
    val inputBg = Color(0xFF150D20)
    val borderColor = Color(0xFF38224D)
    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = pinkAccent,
        uncheckedThumbColor = Color(0xFFB39DDB),
        uncheckedTrackColor = Color(0xFF2E1C44)
    )

    Scaffold(
        containerColor = Color(0xFF100A1A),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAdvanced) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Advanced / API Key",
                            tint = pinkAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF100A1A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─────────────────────────────────────────────
            // MASTER ASSISTANT POWER ON/OFF SWITCH
            // ─────────────────────────────────────────────
            var isAssistantActive by remember {
                mutableStateOf(com.example.ZoyaForegroundService.activeService != null)
            }
            LaunchedEffect(Unit) {
                while (true) {
                    isAssistantActive = (com.example.ZoyaForegroundService.activeService != null)
                    kotlinx.coroutines.delay(500)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAssistantActive) Color(0xFF16251C) else cardBg
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isAssistantActive) Color(0xFF4CAF50) else Color(0xFFE57373).copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isAssistantActive) Color(0xFF2E7D32).copy(alpha = 0.3f) else Color(0xFFC62828).copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isAssistantActive) "⚡" else "💤", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "Maya Assistant Service",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAssistantActive) "Active & Listening (ON)" else "Stopped / Idle (OFF)",
                                color = if (isAssistantActive) Color(0xFF81C784) else Color(0xFFE57373),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Switch(
                        checked = isAssistantActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                com.example.ZoyaForegroundService.start(context)
                                isAssistantActive = true
                                Toast.makeText(context, "Maya Assistant Turned ON", Toast.LENGTH_SHORT).show()
                            } else {
                                com.example.ZoyaForegroundService.stop(context)
                                isAssistantActive = false
                                Toast.makeText(context, "Maya Assistant Turned OFF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF4CAF50),
                            uncheckedThumbColor = Color(0xFFB0A5C4),
                            uncheckedTrackColor = Color(0xFF2E1C44)
                        )
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 🔒 MAYA LOCK & SECURITY SYSTEM
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLock() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Color(0xFF00E5FF).copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔐", fontSize = 20.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Maya Lock System",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFF4081), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Text(
                                text = "PIN, Pattern, One-time Auth & Test Lock ↗",
                                color = Color(0xFF80D8FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open Lock Settings",
                        tint = Color(0xFF80D8FF)
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 0. PERSONA SETTINGS (3 SELECTABLE MODES) 🎭
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, pinkAccent.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Persona Mode", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🎭", fontSize = 15.sp)
                        }

                        Text(
                            text = "Customize",
                            color = pinkAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToPersona() }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Controls tone, vocabulary, teasing level, care & responses.",
                        color = Color(0xFFB0A5C4),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Selectable Persona Cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            PersonaType.CARING_SWEET,
                            PersonaType.PLAYFUL_NAKHRE,
                            PersonaType.SUPER_FRIENDLY
                        ).forEach { persona ->
                            val isSelected = selectedPersona == persona
                            val personaColor = Color(persona.primaryColor)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) personaColor.copy(alpha = 0.15f) else inputBg,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) personaColor else borderColor,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedPersona = persona
                                        PersonaManager.setSelectedPersona(context, persona)
                                        Toast.makeText(context, "${persona.displayName} selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(persona.iconEmoji, fontSize = 20.sp)
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = persona.displayName,
                                                color = if (isSelected) Color.White else Color(0xFFE1D5F5),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("• ACTIVE", color = personaColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = persona.shortDescription,
                                            color = Color(0xFFB0A5C4),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            maxLines = 2
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPersona = persona
                                        PersonaManager.setSelectedPersona(context, persona)
                                        Toast.makeText(context, "${persona.displayName} selected", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = personaColor,
                                        unselectedColor = Color(0xFF5E427B)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 0.1 MAYA HOME HOLOGRAM ANIMATIONS (6 THEMES) 🎯
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Hologram Animation", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🎯", fontSize = 15.sp)
                        }

                        Text(
                            text = "6 Themes",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose your home screen 3D hologram avatar style (Cyber HUD is Default).",
                        color = Color(0xFFB0A5C4),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 6 Selectable Animation Theme Cards
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MayaAnimationType.entries.forEach { anim ->
                            val isSelected = selectedAnimation == anim
                            val animColor = Color(anim.accentColor)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) animColor.copy(alpha = 0.15f) else inputBg,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) animColor else borderColor,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedAnimation = anim
                                        MayaAnimationManager.setSelectedAnimation(context, anim)
                                        Toast.makeText(context, "${anim.displayName} Activated", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(anim.iconEmoji, fontSize = 22.sp)
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = anim.displayName,
                                                color = if (isSelected) Color.White else Color(0xFFE1D5F5),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (anim == MayaAnimationType.CYBER_HUD) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("• DEFAULT", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            } else if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("• ACTIVE", color = animColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = anim.subtitle,
                                            color = Color(0xFFB0A5C4),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            maxLines = 2
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedAnimation = anim
                                        MayaAnimationManager.setSelectedAnimation(context, anim)
                                        Toast.makeText(context, "${anim.displayName} Activated", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = animColor,
                                        unselectedColor = Color(0xFF5E427B)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 1. YOUR NAME 💕 (Screenshot 1)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Your name", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("💕", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = yourName,
                        onValueChange = {
                            yourName = it
                            prefs.edit().putString("boss_name", it).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. RDX sir", color = Color.Gray, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = pinkAccent,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 2. ASSISTANT NAME 🩵 (Screenshot 1)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Assistant name", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🩵", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = assistantName,
                        onValueChange = {
                            assistantName = it
                            prefs.edit().putString("assistant_name", it).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("MAYA", color = Color.Gray, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = pinkAccent,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 3. MUSIC 🎵 (Screenshot 1)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Music", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎵", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // App Selection Pills: YT Music | Spotify | YouTube
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("YT Music", "Spotify", "YouTube").forEach { app ->
                            val isSelected = musicApp.equals(app, ignoreCase = true)
                            Button(
                                onClick = {
                                    musicApp = app
                                    prefs.edit().putString("preferred_music_app", app).apply()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) pinkAccent.copy(alpha = 0.25f) else inputBg
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) pinkAccent else borderColor
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(
                                    text = app,
                                    color = if (isSelected) Color.White else Color(0xFFB39DDB),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = favoriteSong,
                        onValueChange = {
                            favoriteSong = it
                            prefs.edit().putString("favorite_song", it).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Favorite song, e.g. 'Kesariya'", color = Color.Gray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = pinkAccent,
                            unfocusedBorderColor = borderColor,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 4. LANGUAGE 🗣️ (Screenshot 2)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Language", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🗣️", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { languageExpanded = !languageExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { languageExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = pinkAccent,
                                unfocusedBorderColor = borderColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = languageExpanded,
                            onDismissRequest = { languageExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF1F132B))
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        ) {
                            languageOptions.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        selectedLanguage = lang
                                        prefs.edit().putString("app_language", lang).apply()
                                        languageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 5. COUNTRY CODE 🌐 (Screenshot 2)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Country code", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🌐", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCountryCode,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { countryExpanded = !countryExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { countryExpanded = true },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = pinkAccent,
                                unfocusedBorderColor = borderColor,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = countryExpanded,
                            onDismissRequest = { countryExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF1F132B))
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        ) {
                            countryOptions.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country, color = Color.White, fontSize = 13.sp) },
                                    onClick = {
                                        selectedCountryCode = country
                                        prefs.edit().putString("country_code", country).apply()
                                        countryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 6. FAVORITE & SOS CONTACTS 🆘 (Screenshot 2)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Favorite & SOS contacts", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🆘", fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (sosContacts.isEmpty()) {
                        Text("No contacts added yet.", color = Color(0xFFB0A5C4), fontSize = 13.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            sosContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(inputBg, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(contact.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(contact.phone, color = Color(0xFFB39DDB), fontSize = 11.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            SosContactsManager.removeSosContact(context, contact.phone)
                                            sosContacts = SosContactsManager.getSosContacts(context)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "+ Add contact",
                        color = pinkAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                newContactName = ""
                                newContactPhone = ""
                                showAddContactDialog = true
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 7. ECHO GUARD 🛡️ (Screenshot 3)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Echo guard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🛡️", fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Mute the mic while Maya speaks", color = Color(0xFFB0A5C4), fontSize = 12.sp)
                    }
                    Switch(
                        checked = echoGuard,
                        onCheckedChange = {
                            echoGuard = it
                            prefs.edit().putBoolean("echo_guard", it).apply()
                        },
                        colors = switchColors
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 8. PROACTIVE MAYA 💭 (Screenshot 3)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Proactive Maya", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("💭", fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Start conversations on her own", color = Color(0xFFB0A5C4), fontSize = 12.sp)
                    }
                    Switch(
                        checked = proactiveMaya,
                        onCheckedChange = {
                            proactiveMaya = it
                            prefs.edit().putBoolean("proactive_maya", it).apply()
                        },
                        colors = switchColors
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 9. FLOATING ORB 🔥 (Screenshot 3)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Floating orb", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🔥", fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Show over other apps", color = Color(0xFFB0A5C4), fontSize = 12.sp)
                    }
                    Switch(
                        checked = floatingOrb,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Grant 'Display over other apps' permission", Toast.LENGTH_LONG).show()
                                } else {
                                    floatingOrb = true
                                    prefs.edit().putBoolean("floating_orb", true).apply()
                                    FloatingOrbService.start(context)
                                }
                            } else {
                                floatingOrb = false
                                prefs.edit().putBoolean("floating_orb", false).apply()
                                FloatingOrbService.stop(context)
                            }
                        },
                        colors = switchColors
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 10. START ON BOOT 🚀 (Screenshot 3)
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Start on boot", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🚀", fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ready as soon as the phone turns on", color = Color(0xFFB0A5C4), fontSize = 12.sp)
                    }
                    Switch(
                        checked = startOnBoot,
                        onCheckedChange = {
                            startOnBoot = it
                            prefs.edit().putBoolean("start_on_boot", it).apply()
                        },
                        colors = switchColors
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 11. CALL & WHATSAPP ANNOUNCEMENT 📢
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, pinkAccent.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Call Announcement", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📢", fontSize = 15.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Announce incoming Phone & WhatsApp calls", color = Color(0xFFB0A5C4), fontSize = 12.sp)
                        }
                        Switch(
                            checked = callAnnouncementEnabled,
                            onCheckedChange = {
                                callAnnouncementEnabled = it
                                CallAnnouncer.setCallAnnouncementEnabled(context, it)
                            },
                            colors = switchColors
                        )
                    }

                    if (callAnnouncementEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = borderColor)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Phone Calls Announcement", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = phoneCallEnabled,
                                onCheckedChange = {
                                    phoneCallEnabled = it
                                    CallAnnouncer.setPhoneCallAnnouncementEnabled(context, it)
                                },
                                colors = switchColors
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("WhatsApp Calls Announcement", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = whatsappCallEnabled,
                                onCheckedChange = {
                                    whatsappCallEnabled = it
                                    CallAnnouncer.setWhatsAppCallAnnouncementEnabled(context, it)
                                },
                                colors = switchColors
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("WhatsApp Messages Announcement", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = whatsappMsgEnabled,
                                onCheckedChange = {
                                    whatsappMsgEnabled = it
                                    CallAnnouncer.setWhatsAppMessageAnnouncementEnabled(context, it)
                                },
                                colors = switchColors
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SMS Messages Announcement", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = smsMsgEnabled,
                                onCheckedChange = {
                                    smsMsgEnabled = it
                                    CallAnnouncer.setSmsMessageAnnouncementEnabled(context, it)
                                },
                                colors = switchColors
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Repeat while Ringing", color = Color.White, fontSize = 13.sp)
                            Switch(
                                checked = repeatAnnouncement,
                                onCheckedChange = {
                                    repeatAnnouncement = it
                                    CallAnnouncer.setRepeatEnabled(context, it)
                                },
                                colors = switchColors
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { CallAnnouncer.testAnnouncement(context, isWhatsApp = false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, pinkAccent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = pinkAccent)
                            ) {
                                Text("Test Call", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { CallAnnouncer.testMessageAnnouncement(context, isWhatsApp = true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF25D366)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366))
                            ) {
                                Text("Test WA Msg", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { CallAnnouncer.testMessageAnnouncement(context, isWhatsApp = false) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF2196F3)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2196F3))
                            ) {
                                Text("Test SMS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 12. ADVANCED & PERMISSIONS BUTTONS
            // ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToAdvanced),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, pinkAccent.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🔑", fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Advanced", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Gemini API Key", color = Color(0xFFB0A5C4), fontSize = 10.sp)
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onNavigateToPermissions),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛡️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Permissions", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Mic, Calls, Overlays", color = Color(0xFFB0A5C4), fontSize = 10.sp)
                    }
                }
            }
        }
    }

    // Add SOS Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            containerColor = Color(0xFF1F132B),
            title = {
                Text("Add SOS Contact", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Contact Name", color = Color(0xFFB39DDB)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = pinkAccent,
                            unfocusedBorderColor = borderColor
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = { Text("Phone Number", color = Color(0xFFB39DDB)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = pinkAccent,
                            unfocusedBorderColor = borderColor
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                            SosContactsManager.addSosContact(
                                context,
                                SosContact(newContactName.trim(), newContactPhone.trim())
                            )
                            sosContacts = SosContactsManager.getSosContacts(context)
                            showAddContactDialog = false
                            Toast.makeText(context, "SOS Contact added!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = pinkAccent)
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = Color(0xFFB39DDB))
                }
            }
        )
    }
}
