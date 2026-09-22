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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.model.CodeStudioManager
import com.example.model.GeneratedCodeSnippet
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
        composable("maya_home") {
            MayaHomeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("maya_lock") {
            MayaLockScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToPersona = { navController.navigate("persona") },
                onNavigateToLock = { navController.navigate("maya_lock") }
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
    var bossName by remember { mutableStateOf(prefs.getString("boss_name", "shadow x rahul") ?: "shadow x rahul") }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            ZoyaForegroundService.start(context)
            serviceStarted = true
            android.widget.Toast.makeText(context, "Maya Assistant Activated", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Microphone permission required to turn on assistant!", android.widget.Toast.LENGTH_SHORT).show()
        }
        // Refresh weather as soon as location permission is granted
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                com.example.model.LocationWeatherManager.refreshWeather(context, force = true)
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            serviceStarted = (ZoyaForegroundService.activeService != null)
            zoyaState = ZoyaForegroundService.currentState
            delay(500)
        }
    }

    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()
    val activeCode by CodeStudioManager.activeCode.collectAsState()
    val weatherInfo by com.example.model.LocationWeatherManager.weatherState.collectAsState()

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

    // Refresh Live Current Location Weather
    LaunchedEffect(Unit) {
        com.example.model.LocationWeatherManager.refreshWeather(context)
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
                            if (dest.route != "home") {
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
            // 🌟 Full-Screen Live Code Background Wallpaper (Renders website code directly on the home screen behind character and cards)
            FullScreenLiveCodeWallpaper(snippet = activeCode)

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

                    // Assistant Name & Power Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Maya",
                            color = Color(0xFF1E295D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )

                        // Master ON / OFF Toggle Pill
                        val isRunning = serviceStarted || ZoyaForegroundService.activeService != null
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isRunning) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                )
                                .border(
                                    1.dp,
                                    if (isRunning) Color(0xFF4CAF50).copy(alpha = 0.6f) else Color(0xFFE57373).copy(alpha = 0.6f),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (isRunning) {
                                        ZoyaForegroundService.stop(context)
                                        serviceStarted = false
                                        zoyaState = ZoyaState.IDLE
                                        android.widget.Toast.makeText(context, "Maya Assistant turned OFF", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.RECORD_AUDIO,
                                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                                android.Manifest.permission.READ_CONTACTS,
                                                android.Manifest.permission.CALL_PHONE
                                            )
                                        )
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFF2E7D32) else Color(0xFFC62828))
                            )
                            Text(
                                text = if (isRunning) "ON" else "OFF",
                                color = if (isRunning) Color(0xFF2E7D32) else Color(0xFFC62828),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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
                            // Weather Card Top (Live Current Location)
                            Row(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                            val w = com.example.model.LocationWeatherManager.refreshWeather(context, force = true)
                                            withContext(Dispatchers.Main) {
                                                android.widget.Toast.makeText(context, "📍 ${w.cityName}: ${w.temperature} (${w.condition})", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(weatherInfo.icon, fontSize = 16.sp)
                                Column {
                                    Text(weatherInfo.temperature, color = Color(0xFF1E295D), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (weatherInfo.cityName.length > 9) weatherInfo.cityName.take(9) + ".." else weatherInfo.cityName,
                                        color = Color(0xFF1E295D).copy(alpha = 0.7f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
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

                // Sleek Floating Action Bar when code or a website is generated
                if (activeCode != null) {
                    item {
                        val snippet = activeCode!!
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(if (snippet.isWebsite) "🌐" else "⚡", fontSize = 16.sp)
                                Column {
                                    Text(
                                        text = snippet.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (snippet.isWebsite) "Website ready • ${snippet.language}" else "Live Code • ${snippet.language}",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (snippet.filePath != null) {
                                    Button(
                                        onClick = {
                                            val file = java.io.File(snippet.filePath)
                                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val chromeIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "text/html")
                                                setPackage("com.android.chrome")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            try {
                                                context.startActivity(chromeIntent)
                                            } catch (e: Exception) {
                                                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "text/html")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(fallbackIntent)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF238636)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Open Chrome", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText(snippet.title, snippet.code)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF58A6FF), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        CodeStudioManager.clearActiveCode()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Code View", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Centerpiece Cyber Hologram Character
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(310.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CharacterContainer(
                            state = zoyaState,
                            onAnimationClick = null
                        )
                    }
                }

                // Quick Action Buttons Row 1 (Music, Study, Journal)
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

                // Quick Action Buttons Row 2 (Make Website, Coding, Dawa Info)
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionButton(
                            icon = "🌐",
                            label = "Make Web",
                            modifier = Modifier.weight(1f),
                            onClick = { ZoyaForegroundService.activeService?.sendTextMessage("Ek shandar animated website banao aur Chrome me open karo") }
                        )
                        QuickActionButton(
                            icon = "💻",
                            label = "Coding",
                            modifier = Modifier.weight(1f),
                            onClick = { ZoyaForegroundService.activeService?.sendTextMessage("Ek Python coding script likho") }
                        )
                        QuickActionButton(
                            icon = "💊",
                            label = "Dawa Info",
                            modifier = Modifier.weight(1f),
                            onClick = { ZoyaForegroundService.activeService?.sendTextMessage("Paracetamol aur Dolo 650 dawa kis kaam aati hai batao") }
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
                            title = if (weatherInfo.cityName.isEmpty() || weatherInfo.cityName == "Detecting...") "Weather" else "📍 ${weatherInfo.cityName}",
                            value = weatherInfo.temperature,
                            subtitle = weatherInfo.condition,
                            meta = "💧 ${weatherInfo.humidity} • 💨 ${weatherInfo.windSpeed}",
                            icon = weatherInfo.icon,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                                        val w = com.example.model.LocationWeatherManager.refreshWeather(context, force = true)
                                        withContext(Dispatchers.Main) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "📍 ${w.cityName}: ${w.temperature}, ${w.condition} (💧 ${w.humidity})",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
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
            val isAssistantActive = serviceStarted || ZoyaForegroundService.activeService != null
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-20).dp)
                    .size(76.dp)
                    .shadow(12.dp, CircleShape, spotColor = if (isAssistantActive) Color(0xFF00E5FF) else Color.Gray)
                    .clip(CircleShape)
                    .background(
                        if (isAssistantActive) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF00B0FF),
                                    Color(0xFF00E5FF)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF37474F),
                                    Color(0xFF263238)
                                )
                            )
                        }
                    )
                    .clickable {
                        if (isAssistantActive) {
                            ZoyaForegroundService.stop(context)
                            serviceStarted = false
                            zoyaState = ZoyaState.IDLE
                            android.widget.Toast.makeText(context, "Maya Assistant turned OFF", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECORD_AUDIO,
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    android.Manifest.permission.READ_CONTACTS,
                                    android.Manifest.permission.CALL_PHONE
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isAssistantActive) {
                    // Expanding active pulse ring inside
                    CircularTriggerWave(state = zoyaState)
                } else {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Assistant OFF - Tap to Turn ON",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(32.dp)
                    )
                }
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
fun CharacterContainer(
    state: ZoyaState,
    onAnimationClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedAnim by remember {
        mutableStateOf(MayaAnimationManager.getSelectedAnimation(context))
    }

    DisposableEffect(Unit) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "maya_animation_theme") {
                selectedAnim = MayaAnimationManager.getSelectedAnimation(context)
            }
        }
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    val assistantName = remember {
        val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
        prefs.getString("assistant_name", "M.A.Y.A") ?: "M.A.Y.A"
    }

    MayaMasterAnimation(
        state = state,
        animationType = selectedAnim,
        assistantName = assistantName,
        onAnimationClick = onAnimationClick
    )
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

// 🌟 Full-Screen Live Code Background Wallpaper (Renders website/HTML/CSS code with real-time typewriter stream & upward auto-scroll)
@Composable
fun FullScreenLiveCodeWallpaper(snippet: GeneratedCodeSnippet?) {
    val codeText = snippet?.code ?: return
    val snippetId = snippet.id
    val allLines = remember(codeText) { codeText.lines() }
    val totalLines = allLines.size

    // Progressive line count for real-time typewriter stream effect
    var visibleLinesCount by remember(snippetId, codeText) { mutableStateOf(1) }
    var isWritingComplete by remember(snippetId, codeText) { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Real-time typewriter line generation effect
    LaunchedEffect(snippetId, codeText) {
        visibleLinesCount = 1
        isWritingComplete = false
        val delayPerLine = when {
            totalLines > 200 -> 15L
            totalLines > 100 -> 22L
            totalLines > 50 -> 30L
            else -> 40L
        }
        for (i in 1..totalLines) {
            visibleLinesCount = i
            delay(delayPerLine)
        }
        isWritingComplete = true
    }

    // Auto-scroll upward as new lines are typed
    LaunchedEffect(visibleLinesCount) {
        if (!isWritingComplete && scrollState.maxValue > 0) {
            scrollState.animateScrollTo(
                scrollState.maxValue,
                animationSpec = tween(durationMillis = 35, easing = LinearEasing)
            )
        }
    }

    // Blinking live hacker cursor
    val infiniteTransition = rememberInfiniteTransition(label = "liveCursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 80.dp, start = 8.dp, end = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Live synthesizer status header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (!isWritingComplete) Color(0xFF00E5FF) else Color(0xFF69F0AE),
                                CircleShape
                            )
                    )
                    Text(
                        text = if (!isWritingComplete) "⚡ Synthesizing Code... [$visibleLinesCount/$totalLines]" else "✨ Code Synthesized • $totalLines lines",
                        color = if (!isWritingComplete) Color(0xFF00E5FF) else Color(0xFF69F0AE),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isWritingComplete) {
                    Text(
                        text = "LIVE STREAM ⬆",
                        color = Color(0xFFFFD54F),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Streamed lines with upward scrolling
            allLines.take(visibleLinesCount).forEachIndexed { index, rawLine ->
                val isLastActiveLine = (index == visibleLinesCount - 1) && !isWritingComplete
                val lineNumberStr = (index + 1).toString().padStart(3, ' ')

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLastActiveLine) {
                                Modifier.background(
                                    Color(0xFF00E5FF).copy(alpha = 0.15f),
                                    RoundedCornerShape(4.dp)
                                )
                            } else Modifier
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Line Number
                    Text(
                        text = "$lineNumberStr ",
                        color = if (isLastActiveLine) Color(0xFF00E5FF) else Color(0xFF546E7A).copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Line Content with syntax highlighting
                    val annotatedString = buildSyntaxHighlightedLine(rawLine)
                    Text(
                        text = annotatedString,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Live blinking cursor at active typing point
                    if (isLastActiveLine) {
                        Text(
                            text = " ▌",
                            color = Color(0xFF00E5FF).copy(alpha = cursorAlpha),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

fun buildSyntaxHighlightedLine(rawLine: String): AnnotatedString {
    return buildAnnotatedString {
        val trimmed = rawLine.trimStart()
        val leadingSpaces = rawLine.takeWhile { it == ' ' }
        if (leadingSpaces.isNotEmpty()) {
            append(leadingSpaces)
        }

        if (trimmed.startsWith("<!--") || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
            // Comments
            withStyle(SpanStyle(color = Color(0xFF8B949E), fontWeight = FontWeight.Normal)) {
                append(trimmed)
            }
            return@buildAnnotatedString
        }

        var i = 0
        val len = trimmed.length
        while (i < len) {
            val c = trimmed[i]
            when {
                c == '<' -> {
                    // Tag start
                    val tagEnd = trimmed.indexOf('>', i)
                    if (tagEnd != -1) {
                        val fullTag = trimmed.substring(i, tagEnd + 1)
                        parseTagContent(this, fullTag)
                        i = tagEnd + 1
                    } else {
                        withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)) {
                            append(c)
                        }
                        i++
                    }
                }
                c == '"' || c == '\'' -> {
                    val quoteChar = c
                    val nextQuote = trimmed.indexOf(quoteChar, i + 1)
                    if (nextQuote != -1) {
                        val strVal = trimmed.substring(i, nextQuote + 1)
                        withStyle(SpanStyle(color = Color(0xFFFFB74D))) {
                            append(strVal)
                        }
                        i = nextQuote + 1
                    } else {
                        withStyle(SpanStyle(color = Color(0xFFFFB74D))) {
                            append(c)
                        }
                        i++
                    }
                }
                c == '{' || c == '}' || c == '(' || c == ')' || c == '[' || c == ']' || c == ';' || c == ':' -> {
                    withStyle(SpanStyle(color = Color(0xFF80D8FF), fontWeight = FontWeight.Bold)) {
                        append(c)
                    }
                    i++
                }
                else -> {
                    // Check for common programming keywords
                    if (c.isLetter()) {
                        var wordEnd = i
                        while (wordEnd < len && (trimmed[wordEnd].isLetterOrDigit() || trimmed[wordEnd] == '_' || trimmed[wordEnd] == '-')) {
                            wordEnd++
                        }
                        val word = trimmed.substring(i, wordEnd)
                        val keywordColors = when (word) {
                            "const", "let", "var", "function", "fun", "val", "return", "if", "else", "for", "while", "class", "import", "export", "default", "true", "false", "new", "this" -> Color(0xFFFF4081) // Pink/Magenta for keywords
                            "document", "window", "console", "Math", "JSON", "Array", "String", "Object" -> Color(0xFF7C4DFF) // Purple
                            "style", "script", "color", "background", "margin", "padding", "display", "font", "width", "height", "border", "flex", "grid" -> Color(0xFF69F0AE) // Mint Green for CSS
                            else -> Color(0xFFFFFFFF).copy(alpha = 0.95f)
                        }
                        withStyle(SpanStyle(color = keywordColors, fontWeight = if (keywordColors != Color(0xFFFFFFFF).copy(alpha = 0.95f)) FontWeight.Bold else FontWeight.Normal)) {
                            append(word)
                        }
                        i = wordEnd
                    } else {
                        withStyle(SpanStyle(color = Color(0xFFFFFFFF).copy(alpha = 0.95f))) {
                            append(c)
                        }
                        i++
                    }
                }
            }
        }
    }
}

private fun parseTagContent(builder: AnnotatedString.Builder, tagStr: String) {
    var idx = 0
    val len = tagStr.length
    while (idx < len) {
        val ch = tagStr[idx]
        when {
            ch == '<' || ch == '>' || ch == '/' || ch == '!' -> {
                builder.withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)) {
                    append(ch)
                }
                idx++
            }
            ch == '=' -> {
                builder.withStyle(SpanStyle(color = Color(0xFF80D8FF))) {
                    append(ch)
                }
                idx++
            }
            ch == '"' || ch == '\'' -> {
                val q = ch
                val endQ = tagStr.indexOf(q, idx + 1)
                if (endQ != -1) {
                    val stringVal = tagStr.substring(idx, endQ + 1)
                    builder.withStyle(SpanStyle(color = Color(0xFFFFB74D))) { // warm peach/gold string
                        append(stringVal)
                    }
                    idx = endQ + 1
                } else {
                    builder.withStyle(SpanStyle(color = Color(0xFFFFB74D))) {
                        append(ch)
                    }
                    idx++
                }
            }
            ch.isLetter() -> {
                // Word (could be tag name like html, div, or attribute like class, lang)
                var wordEnd = idx
                while (wordEnd < len && (tagStr[wordEnd].isLetterOrDigit() || tagStr[wordEnd] == '-' || tagStr[wordEnd] == '_')) {
                    wordEnd++
                }
                val word = tagStr.substring(idx, wordEnd)
                
                // Determine if it's an attribute or a tag name
                val isTagName = idx <= 2 || tagStr.substring(0, idx).trimEnd().endsWith("<") || tagStr.substring(0, idx).trimEnd().endsWith("</") || tagStr.substring(0, idx).trimEnd().endsWith("<!")
                if (isTagName) {
                    builder.withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                } else {
                    // Attribute name
                    builder.withStyle(SpanStyle(color = Color(0xFF69F0AE), fontWeight = FontWeight.SemiBold)) {
                        append(word)
                    }
                }
                idx = wordEnd
            }
            else -> {
                builder.withStyle(SpanStyle(color = Color(0xFFE0F7FA))) {
                    append(ch)
                }
                idx++
            }
        }
    }
}


