package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ZoyaForegroundService
import com.example.live.ZoyaState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ZoyaScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToDestination = { route ->
                    navController.navigate(route)
                }
            )
        }
        composable("permissions") {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate("permissions") },
                onNavigateToAdvanced = { navController.navigate("advanced") },
                onNavigateToPersona = { navController.navigate("persona") }
            )
        }
        composable("persona") {
            PersonaScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("advanced") {
            AdvancedSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("documents") {
            DocumentsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("study") {
            StudyWhiteboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("privacy") {
            PrivacyPolicyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("scan") {
            CameraScannerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToDestination: (String) -> Unit) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var bossName by remember { mutableStateOf(prefs.getString("boss_name", "Hunter") ?: "Hunter") }
    var showSettingsDialog by remember { mutableStateOf(apiKey.isEmpty()) }
    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            val intent = Intent(context, ZoyaForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
            serviceStarted = true
        } else {
            android.widget.Toast.makeText(context, "Microphone and necessary permissions required!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()

    var temperature by remember { mutableStateOf("28°") }
    var weatherStatus by remember { mutableStateOf("Cloudy") }
    var humidity by remember { mutableStateOf("86%") }
    var weatherIcon by remember { mutableStateOf("☁️") }

    val currentDateVal = remember {
        SimpleDateFormat("d", Locale.getDefault()).format(Date())
    }
    val currentDateSub = remember {
        SimpleDateFormat("E, MMM", Locale.getDefault()).format(Date())
    }

    val currentMood = remember(messages) {
        val lastMsg = messages.lastOrNull()?.lowercase() ?: ""
        when {
            lastMsg.isEmpty() -> "Warm" to "All good"
            lastMsg.contains("happy") || lastMsg.contains("great") || lastMsg.contains("thank") || lastMsg.contains("good") -> "Cheerful" to "Very happy"
            lastMsg.contains("sad") || lastMsg.contains("bad") || lastMsg.contains("error") || lastMsg.contains("fail") -> "Concerned" to "System Alert"
            lastMsg.contains("think") || lastMsg.contains("read") || lastMsg.contains("scan") -> "Focused" to "Analyzing"
            else -> "Warm" to "All good"
        }
    }

    LaunchedEffect(Unit) {
        ZoyaForegroundService.onStateChange = { state ->
            zoyaState = state
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.open-meteo.com/v1/forecast?latitude=28.6139&longitude=77.2090&current=temperature_2m,relative_humidity_2m,weather_code")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val tempIndex = response.indexOf("\"temperature_2m\":")
                    if (tempIndex != -1) {
                        val tempSub = response.substring(tempIndex + 17)
                        val tempVal = tempSub.takeWhile { it != ',' && it != '}' }.trim()
                        temperature = "${tempVal.toDouble().toInt()}°"
                    }
                    val humidityIndex = response.indexOf("\"relative_humidity_2m\":")
                    if (humidityIndex != -1) {
                        val humSub = response.substring(humidityIndex + 23)
                        val humVal = humSub.takeWhile { it != ',' && it != '}' }.trim()
                        humidity = "$humVal%"
                    }
                    val codeIndex = response.indexOf("\"weather_code\":")
                    if (codeIndex != -1) {
                        val codeSub = response.substring(codeIndex + 15)
                        val codeVal = codeSub.takeWhile { it != ',' && it != '}' }.trim().toIntOrNull() ?: 0
                        val (status, icon) = when (codeVal) {
                            0 -> "Clear" to "☀️"
                            1, 2, 3 -> "Partly Cloudy" to "⛅"
                            45, 48 -> "Foggy" to "🌫️"
                            51, 53, 55 -> "Drizzle" to "🌧️"
                            61, 63, 65 -> "Rainy" to "🌧️"
                            71, 73, 75 -> "Snowy" to "❄️"
                            80, 81, 82 -> "Showers" to "🌦️"
                            95, 96, 99 -> "Thunderstorm" to "⛈️"
                            else -> "Cloudy" to "☁️"
                        }
                        weatherStatus = status
                        weatherIcon = icon
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Determine Greeting time
    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF160D20),
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                ZoyaDrawerContent(
                    currentRoute = "home",
                    onNavigate = { dest ->
                        coroutineScope.launch {
                            drawerState.close()
                            if (dest.route != "home" && dest.route != "maya_home") {
                                onNavigateToDestination(dest.route)
                            }
                        }
                    }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE3F2FD), // Bright light sky blue
                            Color(0xFFBBDEFB), // Soft cyan blue ambient lighting
                            Color(0xFF1E295D)  // Deep navy blue at the very bottom
                        ),
                        startY = 0f,
                        endY = 2200f
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 1. Transparent Header exactly like reference image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hamburger Menu (Opens the exact Maya drawer from the screenshot)
                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF1E295D)
                        )
                    }

                    // Assistant Name Centered: "Maya"
                    Text(
                        text = "Maya",
                        color = Color(0xFF1E295D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 0.5.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Notification / Permissions shortcut indicator
                        IconButton(onClick = { onNavigateToDestination("permissions") }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Permissions & Notifications",
                                tint = Color(0xFF1E295D)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        // Profile Bubble
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E295D))
                                .clickable { onNavigateToDestination("settings") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bossName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Greeting and Widgets Side-by-Side row
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "$greetingText,",
                                color = Color(0xFF1E295D).copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = bossName,
                                color = Color(0xFF1E295D),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (zoyaState) {
                                    ZoyaState.LISTENING -> "Listening..."
                                    ZoyaState.THINKING -> "Thinking..."
                                    ZoyaState.SPEAKING -> "Speaking..."
                                    else -> "Ready to assist"
                                },
                                color = Color(0xFF1E295D).copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Right-hand status widget cards
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            // Weather Card Top
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(weatherIcon, fontSize = 16.sp)
                                Column {
                                    Text(temperature, color = Color(0xFF1E295D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(weatherStatus, color = Color(0xFF1E295D).copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                            }

                            // Energy/Status Card Top
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⚡", fontSize = 14.sp)
                                Column {
                                    Text("10", color = Color(0xFF1E295D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Energy", color = Color(0xFF1E295D).copy(alpha = 0.6f), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }

                // Centerpiece Witch Character
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CharacterContainer(state = zoyaState)
                    }
                }

                // Quick Action Buttons Row (Music, Study, Journal) exactly like reference image
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            icon = "🎵",
                            label = "Music",
                            modifier = Modifier.weight(1f),
                            onClick = { ZoyaForegroundService.activeService?.sendTextMessage("Play some music") }
                        )
                        QuickActionButton(
                            icon = "📖",
                            label = "Study",
                            modifier = Modifier.weight(1f),
                            onClick = { ZoyaForegroundService.activeService?.sendTextMessage("Open my study goals") }
                        )
                        QuickActionButton(
                            icon = "✏️",
                            label = "Journal",
                            modifier = Modifier.weight(1f),
                            onClick = { ZoyaForegroundService.activeService?.sendTextMessage("Let's write a journal entry") }
                        )
                    }
                }

                // Info Glass Cards Grid (Weather, Today, Mood)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InfoGlassCard(
                            title = "Weather",
                            value = temperature,
                            subtitle = weatherStatus,
                            meta = "💧 $humidity",
                            icon = weatherIcon,
                            modifier = Modifier.weight(1f)
                        )
                        InfoGlassCard(
                            title = "Today",
                            value = currentDateVal,
                            subtitle = currentDateSub,
                            meta = "Optimized",
                            icon = "📅",
                            modifier = Modifier.weight(1f)
                        )
                        InfoGlassCard(
                            title = "Mood",
                            value = currentMood.first,
                            subtitle = currentMood.second,
                            meta = "Peaceful",
                            icon = "❤️",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Large Premium Input Bar "Ask Maya anything..."
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    var inputText by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(28.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment Clip button
                        Icon(
                            imageVector = Icons.Default.Share, // acts as clip/attachment
                            contentDescription = "Attachment",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    ZoyaForegroundService.activeService?.sendTextMessage("Check my screen")
                                }
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        // Text Field Input
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (inputText.isEmpty()) {
                                Text(
                                    text = "Ask Maya anything...",
                                    color = Color.Gray,
                                    fontSize = 15.sp
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.Black,
                                    fontSize = 15.sp
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Send Arrow Button
                        IconButton(
                            onClick = {
                                if (inputText.isNotEmpty()) {
                                    ZoyaForegroundService.activeService?.sendTextMessage(inputText)
                                    inputText = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF1E295D)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp)) // clearance for bottom navigation
                }
            }
        }

        // Bottom Navigation Bar exactly styled like the image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF1E295D).copy(alpha = 0.95f))
                    )
                )
                .padding(bottom = 12.dp)
        ) {
            // Glass Navigation Bar Background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(68.dp)
                    .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(34.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(34.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Home
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { /* Active home */ }
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFF00B0FF))
                        Text("Home", color = Color(0xFF00B0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Scan
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onNavigateToDestination("scan")
                        }
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan", tint = Color(0xFF00B0FF))
                        Text("Scan", color = Color(0xFF00B0FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Gap for Elevated circle voice trigger
                    Spacer(modifier = Modifier.width(64.dp))

                    // Memories
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onNavigateToDestination("documents")
                        }
                    ) {
                        Icon(Icons.Default.Star, contentDescription = "Memories", tint = Color.Gray)
                        Text("Memories", color = Color.Gray, fontSize = 10.sp)
                    }

                    // Chat logs
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onNavigateToDestination("chat") }
                    ) {
                        Icon(Icons.Default.MailOutline, contentDescription = "Chat", tint = Color.Gray)
                        Text("Chat", color = Color.Gray, fontSize = 10.sp)
                    }
                }
            }

            // Visually Dominant Circular Floating Mic trigger exactly like reference image
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .size(76.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color(0xFF00E5FF))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00B0FF),
                                Color(0xFF00E5FF)
                            )
                        )
                    )
                    .clickable {
                        val service = ZoyaForegroundService.activeService
                        if (service != null) {
                            service.reconnectSession()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECORD_AUDIO,
                                    android.Manifest.permission.READ_CONTACTS,
                                    android.Manifest.permission.CALL_PHONE
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Expanding active pulse ring inside
                CircularTriggerWave(state = zoyaState)
            }
        }
    }
}

    // Settings Configuration Dialog
    if (showSettingsDialog) {
        var tempKey by remember { mutableStateOf(apiKey) }
        var tempBossName by remember { mutableStateOf(bossName) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    "System Settings",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            containerColor = Color.White,
            text = {
                Column {
                    Text("Configure parameters for Zoya AI.", color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Boss Name:", color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = tempBossName,
                        onValueChange = { tempBossName = it },
                        placeholder = { Text("e.g. Boss, Lakshya") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color(0xFFF1F3F4),
                            unfocusedContainerColor = Color(0xFFF1F3F4)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Gemini API Key:", color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    TextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        placeholder = { Text("AIzaSy...") },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color(0xFFF1F3F4),
                            unfocusedContainerColor = Color(0xFFF1F3F4)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Get your Gemini API Key here",
                        color = Color(0xFF00B0FF),
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hands-Free Services Access:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("🔔 Notification Listener Access", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("⚡ Automation Accessibility Service", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.edit()
                            .putString("api_key", tempKey)
                            .putString("boss_name", tempBossName)
                            .apply()
                        apiKey = tempKey
                        bossName = tempBossName
                        showSettingsDialog = false
                    }
                ) {
                    Text("Save Changes", color = Color(0xFF00B0FF), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun CharacterContainer(state: ZoyaState) {
    val infiniteTransition = rememberInfiniteTransition()

    // Base breathing/pulsing scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Main orb rotation
    val orbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Inner wave offset for reactive states
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Core glowing colors corresponding to each state
        val primaryColor = when (state) {
            ZoyaState.LISTENING -> Color(0xFFB388FF) // Purple
            ZoyaState.THINKING -> Color(0xFFFFD180)  // Golden
            ZoyaState.SPEAKING -> Color(0xFF00E676)  // Emerald
            else -> Color(0xFF00E5FF)                // Electric Cyan
        }

        val secondaryColor = when (state) {
            ZoyaState.LISTENING -> Color(0xFF7C4DFF)
            ZoyaState.THINKING -> Color(0xFFFF9100)
            ZoyaState.SPEAKING -> Color(0xFF00B0FF)
            else -> Color(0xFF2979FF)
        }

        // Draw the futuristic professional vector hologram sphere
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2.5f) * pulseScale

            // 1. Outer Ambient Aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.35f),
                        secondaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.8f
                ),
                radius = baseRadius * 1.8f
            )

            // 2. Layered Holographic Orbital Rings
            val ringCount = 3
            for (i in 0 until ringCount) {
                val angleOffset = (i * 120) + (orbRotation * (if (i % 2 == 0) 1 else -1))
                rotate(degrees = angleOffset, pivot = center) {
                    drawOval(
                        brush = Brush.linearGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.6f), Color.Transparent, secondaryColor.copy(alpha = 0.6f))
                        ),
                        topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 0.25f),
                        size = Size(baseRadius * 2.5f, baseRadius * 0.5f),
                        style = Stroke(width = 3f)
                    )

                    // Accent particle nodes along orbits
                    drawCircle(
                        color = primaryColor,
                        radius = 6f,
                        center = Offset(center.x + baseRadius * 1.25f, center.y)
                    )
                }
            }

            // 3. Central Quantum Sphere Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor.copy(alpha = 0.8f),
                        secondaryColor.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.75f
                ),
                radius = baseRadius * 0.75f
            )

            // Inner Tech Circuit Lines inside core
            drawCircle(
                color = primaryColor.copy(alpha = 0.7f),
                radius = baseRadius * 0.55f,
                style = Stroke(width = 1.5f)
            )

            // 4. Dynamic Audio-reactive/Thinking Sine waves crossing the core horizontally
            val wavePoints = 40
            val path = Path()
            val amplitude = if (state == ZoyaState.SPEAKING || state == ZoyaState.LISTENING) 25f else 10f
            val frequency = if (state == ZoyaState.THINKING) 0.25f else 0.15f

            for (p in 0..wavePoints) {
                val fraction = p.toFloat() / wavePoints
                val x = center.x - baseRadius * 0.6f + (baseRadius * 1.2f * fraction)
                val angle = (fraction * 2f * Math.PI.toFloat() * frequency * 10f) + waveOffset
                val y = center.y + (amplitude * kotlin.math.sin(angle))

                if (p == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = 3.5f)
            )

            // 5. Surrounding Cybernetic Tech Ring
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = baseRadius * 1.15f,
                style = Stroke(width = 4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f))
            )
        }
    }
}

@Composable
fun CircularTriggerWave(state: ZoyaState) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (state == ZoyaState.LISTENING || state == ZoyaState.SPEAKING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.3f * (1.7f - waveScale)), CircleShape)
            )
        }
        // Floating white square inside active state, or microphone icon
        if (state == ZoyaState.LISTENING || state == ZoyaState.SPEAKING) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Default.PlayArrow, // play/mic state
                contentDescription = "Trigger Voice",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = Color(0xFF1E295D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoGlassCard(
    title: String,
    value: String,
    subtitle: String,
    meta: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(icon, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color(0xFF1E295D),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFF1E295D).copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = meta,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()

    Scaffold(
        containerColor = Color(0xFF0B0D19),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Back", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text("Operational Logs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "State: ${ZoyaForegroundService.currentState.name}",
                color = Color(0xFF00B0FF),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    Text(
                        text = message,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { ZoyaForegroundService.activeService?.reconnectSession() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B0FF)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Reconnect Uplink", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

