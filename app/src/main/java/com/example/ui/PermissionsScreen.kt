package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.accessibility.ZoyaAccessibilityService
import com.example.notification.ZoyaNotificationListenerService

data class PermissionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isSpecial: Boolean = false,
    val isGranted: (Context) -> Boolean,
    val onRequest: (Context, () -> Unit) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trigger state to re-evaluate permission statuses when returning from settings
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Standard runtime permissions launcher
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    val permissionList = remember {
        listOf(
            PermissionItem(
                id = "audio",
                title = "Microphone (Voice Streaming)",
                description = "Required for hands-free real-time conversation with Zoya / Maya AI.",
                icon = Icons.Default.Mic,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            ),
            PermissionItem(
                id = "contacts_read",
                title = "Read Contacts",
                description = "Allows finding contacts by name/nickname for WhatsApp & phone calls.",
                icon = Icons.Default.AccountBox,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                }
            ),
            PermissionItem(
                id = "contacts_write",
                title = "Save Contacts",
                description = "Allows saving new contact numbers naturally via voice commands.",
                icon = Icons.Default.PersonAdd,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.WRITE_CONTACTS))
                }
            ),
            PermissionItem(
                id = "call_phone",
                title = "Direct Phone Calling",
                description = "Required to make phone calls automatically upon voice command.",
                icon = Icons.Default.Phone,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                }
            ),
            PermissionItem(
                id = "phone_state",
                title = "Phone State & Incoming Calls",
                description = "Detects when a phone call is ringing to trigger Maya's live spoken announcement.",
                icon = Icons.Default.PhoneAndroid,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
                }
            ),
            PermissionItem(
                id = "call_log",
                title = "Read Caller ID & Call Log",
                description = "Enables Maya to read the incoming phone number / caller identity to announce names.",
                icon = Icons.Default.CallReceived,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG))
                }
            ),
            PermissionItem(
                id = "answer_calls",
                title = "Answer / End Incoming Calls",
                description = "Enables 'Utha lo' and 'Kaat do' hands-free call control.",
                icon = Icons.Default.Call,
                isGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
                    } else true
                },
                onRequest = { ctx, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        multiplePermissionLauncher.launch(arrayOf(Manifest.permission.ANSWER_PHONE_CALLS))
                    }
                }
            ),
            PermissionItem(
                id = "sms",
                title = "Send SMS",
                description = "Required for hands-free SMS text message sending.",
                icon = Icons.Default.Email,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.SEND_SMS))
                }
            ),
            PermissionItem(
                id = "notifications",
                title = "Post Notifications",
                description = "Keeps Zoya's foreground service and active status visible.",
                icon = Icons.Default.Notifications,
                isGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true
                },
                onRequest = { ctx, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        multiplePermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            ),
            PermissionItem(
                id = "notif_listener",
                title = "Notification Listener Access",
                description = "Monitors incoming WhatsApp, SMS, and Call notifications for voice alerts.",
                icon = Icons.Default.NotificationsActive,
                isSpecial = true,
                isGranted = { ctx ->
                    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
                    flat != null && flat.contains(ctx.packageName)
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionItem(
                id = "accessibility",
                title = "Accessibility Automation Service",
                description = "Required for auto-typing and sending WhatsApp messages and screen reading.",
                icon = Icons.Default.TouchApp,
                isSpecial = true,
                isGranted = { ctx ->
                    ZoyaAccessibilityService.instance != null
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionItem(
                id = "write_settings",
                title = "Modify System Settings",
                description = "Required for controlling device screen brightness directly.",
                icon = Icons.Default.Brightness6,
                isSpecial = true,
                isGranted = { ctx ->
                    Settings.System.canWrite(ctx)
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionItem(
                id = "battery",
                title = "Ignore Battery Optimization",
                description = "Prevents Android from killing Zoya background voice assistant service.",
                icon = Icons.Default.BatteryChargingFull,
                isSpecial = true,
                isGranted = { ctx ->
                    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                    pm.isIgnoringBatteryOptimizations(ctx.packageName)
                },
                onRequest = { ctx, _ ->
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${ctx.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    ctx.startActivity(intent)
                }
            ),
            PermissionItem(
                id = "overlay",
                title = "Display Over Other Apps (Floating Orb)",
                description = "Required to show Maya's floating interactive orb over any active app.",
                icon = Icons.Default.Layers,
                isSpecial = true,
                isGranted = { ctx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Settings.canDrawOverlays(ctx)
                    } else true
                },
                onRequest = { ctx, _ ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${ctx.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        ctx.startActivity(intent)
                    }
                }
            ),
            PermissionItem(
                id = "camera",
                title = "Camera & Flashlight",
                description = "Required for toggling torch and future visual scanning.",
                icon = Icons.Default.CameraAlt,
                isGranted = { ctx ->
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                },
                onRequest = { ctx, _ ->
                    multiplePermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            )
        )
    }

    Scaffold(
        containerColor = Color(0xFF140D1F), // Rich dark plum matching the drawer theme
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Permissions Hub",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Manage all Android access & services",
                            color = Color(0xFFB39DDB),
                            fontSize = 12.sp
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
                actions = {
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("App Settings", color = Color(0xFFFF6584), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F132B)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Master Grant Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2A1B3D)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6584).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Hands-Free Complete Setup",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Grant runtime permissions in 1-tap",
                                    color = Color(0xFFE1BEE7),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.READ_CONTACTS,
                                    Manifest.permission.WRITE_CONTACTS,
                                    Manifest.permission.CALL_PHONE,
                                    Manifest.permission.READ_PHONE_STATE,
                                    Manifest.permission.READ_CALL_LOG,
                                    Manifest.permission.SEND_SMS,
                                    Manifest.permission.CAMERA
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    perms.add(Manifest.permission.ANSWER_PHONE_CALLS)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                multiplePermissionLauncher.launch(perms.toTypedArray())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF6584)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Grant All Standard Permissions", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(permissionList, key = { it.id }) { item ->
                val isGranted = item.isGranted(context)
                // Reference refreshTrigger to recompose on resume
                val dummy = refreshTrigger

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isGranted) Color(0xFF1C132B) else Color(0xFF261836)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGranted) Color(0xFF00E676).copy(alpha = 0.3f) else Color(0xFFFF9800).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isGranted) Color(0xFF00E676).copy(alpha = 0.15f)
                                    else Color(0xFFFF9800).copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isGranted) Color(0xFF00E676) else Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                color = Color(0xFFB0A5C4),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isGranted) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Granted",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Granted",
                                        color = Color(0xFF00E676),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    item.onRequest(context) { refreshTrigger++ }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF6584)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    if (item.isSpecial) "Enable" else "Grant",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
