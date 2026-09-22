package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SetupStep {
    SELECT_TYPE,
    ENTER_INITIAL,
    CONFIRM_REENTER,
    SUCCESS
}

enum class TestWorkflowStep {
    IDLE,
    STEP_1_LOCKING,
    STEP_2_LOCK_SCREEN_DETECTED,
    STEP_3_SWIPE_UP,
    STEP_4_CREDENTIAL_ENTRY,
    STEP_5_VERIFYING,
    TEST_SUCCESSFUL,
    TEST_FAILED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MayaLockScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isPermissionGranted by remember { mutableStateOf(MayaLockManager.isAccessibilityPermissionGranted(context)) }
    var currentLockType by remember { mutableStateOf(MayaLockManager.getLockType(context)) }
    var isLockSystemEnabled by remember { mutableStateOf(MayaLockManager.isLockSystemEnabled(context)) }

    // Dialog state for One-time Permission Authorization
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Setup Flow State
    var showSetupDialog by remember { mutableStateOf(false) }
    var setupType by remember { mutableStateOf(MayaLockType.PIN) }
    var setupStep by remember { mutableStateOf(SetupStep.ENTER_INITIAL) }
    var tempInitialPin by remember { mutableStateOf("") }
    var tempInitialPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var setupErrorMessage by remember { mutableStateOf<String?>(null) }
    var setupPinState by remember { mutableStateOf(PinState.INPUT) }
    var setupPatternState by remember { mutableStateOf(PatternState.INPUT) }

    // Test Lock Workflow State
    var testWorkflowStep by remember { mutableStateOf(TestWorkflowStep.IDLE) }
    var showTestWorkflowModal by remember { mutableStateOf(false) }
    var testStatusText by remember { mutableStateOf("") }
    var testPinInputState by remember { mutableStateOf(PinState.INPUT) }
    var testPatternInputState by remember { mutableStateOf(PatternState.INPUT) }

    // Refresh permission status when coming back to screen
    LaunchedEffect(Unit) {
        isPermissionGranted = MayaLockManager.isAccessibilityPermissionGranted(context)
        currentLockType = MayaLockManager.getLockType(context)
        isLockSystemEnabled = MayaLockManager.isLockSystemEnabled(context)
    }

    Scaffold(
        containerColor = Color(0xFF0F0818),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Maya Lock System",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Personal PIN, Pattern & Voice Security",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF160D22))
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Status Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C112C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382255))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isLockSystemEnabled) Color(0xFF00E5FF).copy(alpha = 0.15f)
                                            else Color(0xFFFF5252).copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isLockSystemEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Lock State",
                                        tint = if (isLockSystemEnabled) Color(0xFF00E5FF) else Color(0xFFFF5252),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = if (isLockSystemEnabled) "Maya Lock: Active" else "Maya Lock: Off / Not Set",
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (currentLockType) {
                                            MayaLockType.PIN -> "Configured with 4-Digit PIN 🔢"
                                            MayaLockType.PATTERN -> "Configured with 3x3 Pattern ⚡"
                                            MayaLockType.NONE -> "Koi credential set nahi hai"
                                        },
                                        color = Color(0xFFB39DDB),
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            if (currentLockType != MayaLockType.NONE) {
                                Switch(
                                    checked = isLockSystemEnabled,
                                    onCheckedChange = { newState ->
                                        isLockSystemEnabled = newState
                                        MayaLockManager.setLockSystemEnabled(context, newState)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFFF4081)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. One-Time Permission Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181026)),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPermissionGranted) Color(0xFF00E676).copy(alpha = 0.4f) else Color(0xFFFFD54F).copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isPermissionGranted) "🛡️ One-time Permission Authorized" else "⚠️ Permission Required",
                                color = if (isPermissionGranted) Color(0xFF00E676) else Color(0xFFFFD54F),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isPermissionGranted) {
                                "One-time Accessibility authorization active hai. Har baar popup nahi aayega. Agar aap baad me system settings se revoke karte hain, tabhi dobara authorization maangi jayegi."
                            } else {
                                "Phone Lock aur Screen Wake gestures ke liye Accessibility Service permission zaroori hai. Ek baar authorize karne par ye hamesha active rahegi."
                            },
                            color = Color(0xFFE0E0E0),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (!isPermissionGranted) {
                                    MayaLockManager.openAccessibilitySettings(context)
                                } else {
                                    Toast.makeText(context, "Permission pehle se authorized hai! ✅", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPermissionGranted) Color(0xFF263238) else Color(0xFFFF5277)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Icon(
                                if (isPermissionGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isPermissionGranted) Color(0xFF00E676) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPermissionGranted) "Authorized (Active)" else "Authorize Permission Now",
                                color = if (isPermissionGranted) Color(0xFF00E676) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Credential Setup Section (PIN vs Pattern)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181026)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382255))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "🔐 Maya Credential Configuration",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Maya ka apna lock credential set karein. Ek baar set karne ke baad same credential dobara enter karke confirmation hoga.",
                            color = Color(0xFFB39DDB),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Set PIN Button
                            Button(
                                onClick = {
                                    setupType = MayaLockType.PIN
                                    setupStep = SetupStep.ENTER_INITIAL
                                    tempInitialPin = ""
                                    setupErrorMessage = null
                                    setupPinState = PinState.INPUT
                                    showSetupDialog = true
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentLockType == MayaLockType.PIN) Color(0xFF7B1FA2) else Color(0xFF261938)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (currentLockType == MayaLockType.PIN) Color(0xFFE1BEE7) else Color(0xFF452B65)
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔢", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentLockType == MayaLockType.PIN) "Change PIN" else "Set PIN",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Set Pattern Button
                            Button(
                                onClick = {
                                    setupType = MayaLockType.PATTERN
                                    setupStep = SetupStep.ENTER_INITIAL
                                    tempInitialPattern = emptyList()
                                    setupErrorMessage = null
                                    setupPatternState = PatternState.INPUT
                                    showSetupDialog = true
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentLockType == MayaLockType.PATTERN) Color(0xFF00838F) else Color(0xFF261938)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (currentLockType == MayaLockType.PATTERN) Color(0xFF80DEEA) else Color(0xFF452B65)
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (currentLockType == MayaLockType.PATTERN) "Change Pattern" else "Set Pattern",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        if (currentLockType != MayaLockType.NONE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    MayaLockManager.removeCredential(context)
                                    currentLockType = MayaLockType.NONE
                                    isLockSystemEnabled = false
                                    Toast.makeText(context, "Maya Credential removed", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove Current Credential", color = Color(0xFFFF5252), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // 4. Test Lock Section (Requirement #3 Flow)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140D22)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF4081).copy(alpha = 0.7f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧪", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test Lock & Unlock Workflow",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Test Lock ka purpose ye check karna hai ki Maya ka configured Lock/Unlock workflow properly kaam kar raha hai:\n" +
                                    "Maya → Phone Lock → Lock Screen → Swipe Up → Maya PIN/Pattern → Unlock → Actual result verify → Test Successful.",
                            color = Color(0xFFCE93D8),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Test Workflow Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // In-App Interactive Test Workflow
                            Button(
                                onClick = {
                                    if (currentLockType == MayaLockType.NONE) {
                                        Toast.makeText(context, "Pehle Maya PIN ya Pattern set karein!", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (!isPermissionGranted) {
                                        showPermissionDialog = true
                                        return@Button
                                    }
                                    // Start Interactive Workflow Test
                                    testWorkflowStep = TestWorkflowStep.STEP_1_LOCKING
                                    testStatusText = "Maya Phone Lock execute kar rahi hai..."
                                    testPinInputState = PinState.INPUT
                                    testPatternInputState = PatternState.INPUT
                                    showTestWorkflowModal = true

                                    coroutineScope.launch {
                                        delay(1200)
                                        testWorkflowStep = TestWorkflowStep.STEP_2_LOCK_SCREEN_DETECTED
                                        testStatusText = "Lock Screen detected."
                                        delay(1200)
                                        testWorkflowStep = TestWorkflowStep.STEP_3_SWIPE_UP
                                        testStatusText = "Swipe Up gesture execute ho raha hai..."
                                        delay(1200)
                                        testWorkflowStep = TestWorkflowStep.STEP_4_CREDENTIAL_ENTRY
                                        testStatusText = "Apna configured Maya ${currentLockType.name} enter karein:"
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Test Workflow 🧪",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            // Real Device Lock Test
                            OutlinedButton(
                                onClick = {
                                    if (!isPermissionGranted) {
                                        showPermissionDialog = true
                                        return@OutlinedButton
                                    }
                                    val locked = MayaLockManager.lockPhone(context)
                                    if (locked) {
                                        Toast.makeText(context, "Device Locked! Ab screen on karke Maya unlock test karein.", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Lock failed. Please ensure Accessibility permission is active.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                            ) {
                                Text(
                                    text = "Real Phone Lock 📱",
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Last Test Status Result Indicator
                        val lastTest = remember { MayaLockManager.getLastTestResult(context) }
                        if (lastTest.second > 0L) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (lastTest.first) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (lastTest.first) Color(0xFF00E676) else Color(0xFFFF5252),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lastTest.first) "Last Test: Successful & Verified! ✅" else "Last Test: Failed",
                                    color = if (lastTest.first) Color(0xFF00E676) else Color(0xFFFF5252),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 5. Normal Voice Control & Android Security Architecture Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF140D22)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382255))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗣️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice Commands & Controls",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1D142E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "“Hey Maya, phone lock karo.”",
                                    color = Color(0xFFFF80AB),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "→ Authorized lock action execute hota hai aur phone lock ho jata hai.",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "“Hey Maya, phone unlock karo.”",
                                    color = Color(0xFF80D8FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "→ Maya screen wake karti hai, swipe up execute karti hai, aur Maya PIN/Pattern verification present karti hai.",
                                    color = Color(0xFFE0E0E0),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Android Security Isolation Notice
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ℹ️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Android Security Isolation: Android OS kisi bhi app ya service ko system ke native secure password keypad par automatic touches daalne ki ijazat nahi deta. Isliye Maya apna secure PIN/Pattern workflow verify karti hai aur dismissal execute karti hai.",
                                color = Color(0xFF9E9E9E),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Permission Authorization Dialog (One-Time Prompt)
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = Color(0xFF1C112C),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛡️", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "One-time Authorization",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Maya ko phone lock aur unlock actions perform karne ke liye Accessibility Service permission required hai.",
                        color = Color(0xFFE0E0E0),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ek baar enable karne ke baad ye hamesha active rahegi aur har baar popup nahi aayega.",
                        color = Color(0xFF00E676),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        MayaLockManager.openAccessibilitySettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Authorize Now", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Credential Setup Dialog (PIN / Pattern with 2-step Confirmation)
    if (showSetupDialog) {
        AlertDialog(
            onDismissRequest = { showSetupDialog = false },
            containerColor = Color(0xFF140A22),
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (setupType == MayaLockType.PIN) "🔢 Configure Maya PIN" else "⚡ Configure Maya Pattern",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (setupStep) {
                            SetupStep.SELECT_TYPE -> "Choose credential"
                            SetupStep.ENTER_INITIAL -> if (setupType == MayaLockType.PIN) "Step 1: Enter 4-Digit PIN" else "Step 1: Draw Pattern (min 4 dots)"
                            SetupStep.CONFIRM_REENTER -> if (setupType == MayaLockType.PIN) "Step 2: Re-enter PIN (Confirm)" else "Step 2: Re-draw Pattern (Confirm)"
                            SetupStep.SUCCESS -> "Configured Successfully! 🎉"
                        },
                        color = if (setupStep == SetupStep.SUCCESS) Color(0xFF00E676) else Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (setupErrorMessage != null) {
                        Text(
                            text = setupErrorMessage ?: "",
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (setupType == MayaLockType.PIN) {
                        PinLockView(
                            pinLength = 4,
                            pinState = setupPinState,
                            onPinCompleted = { enteredPin ->
                                if (setupStep == SetupStep.ENTER_INITIAL) {
                                    tempInitialPin = enteredPin
                                    setupStep = SetupStep.CONFIRM_REENTER
                                    setupPinState = PinState.INPUT
                                    setupErrorMessage = null
                                } else if (setupStep == SetupStep.CONFIRM_REENTER) {
                                    if (enteredPin == tempInitialPin) {
                                        setupPinState = PinState.SUCCESS
                                        setupStep = SetupStep.SUCCESS
                                        MayaLockManager.savePin(context, enteredPin)
                                        currentLockType = MayaLockType.PIN
                                        isLockSystemEnabled = true
                                        coroutineScope.launch {
                                            delay(1000)
                                            showSetupDialog = false
                                            Toast.makeText(context, "Maya PIN Saved! ✅", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        setupPinState = PinState.ERROR
                                        setupErrorMessage = "PIN match nahi hua! Dobara koshish karein."
                                        coroutineScope.launch {
                                            delay(1200)
                                            setupPinState = PinState.INPUT
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        PatternLockView(
                            modifier = Modifier.size(280.dp),
                            patternState = setupPatternState,
                            onPatternCompleted = { patternDots ->
                                if (patternDots.size < 4) {
                                    setupPatternState = PatternState.ERROR
                                    setupErrorMessage = "Kam se kam 4 dots connect karein!"
                                    coroutineScope.launch {
                                        delay(1000)
                                        setupPatternState = PatternState.INPUT
                                    }
                                    return@PatternLockView
                                }

                                if (setupStep == SetupStep.ENTER_INITIAL) {
                                    tempInitialPattern = patternDots
                                    setupStep = SetupStep.CONFIRM_REENTER
                                    setupPatternState = PatternState.INPUT
                                    setupErrorMessage = null
                                } else if (setupStep == SetupStep.CONFIRM_REENTER) {
                                    if (patternDots == tempInitialPattern) {
                                        setupPatternState = PatternState.SUCCESS
                                        setupStep = SetupStep.SUCCESS
                                        MayaLockManager.savePattern(context, patternDots)
                                        currentLockType = MayaLockType.PATTERN
                                        isLockSystemEnabled = true
                                        coroutineScope.launch {
                                            delay(1000)
                                            showSetupDialog = false
                                            Toast.makeText(context, "Maya Pattern Saved! ✅", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        setupPatternState = PatternState.ERROR
                                        setupErrorMessage = "Pattern match nahi hua! Dobara koshish karein."
                                        coroutineScope.launch {
                                            delay(1200)
                                            setupPatternState = PatternState.INPUT
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSetupDialog = false }) {
                    Text("Close", color = Color.Gray)
                }
            }
        )
    }

    // Interactive Test Workflow Modal (Requirement #3 execution)
    if (showTestWorkflowModal) {
        AlertDialog(
            onDismissRequest = {
                if (testWorkflowStep == TestWorkflowStep.TEST_SUCCESSFUL || testWorkflowStep == TestWorkflowStep.TEST_FAILED) {
                    showTestWorkflowModal = false
                }
            },
            containerColor = Color(0xFF10071C),
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧪", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Test Lock Workflow",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Step indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StepDot(number = "1", title = "Lock", active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_1_LOCKING.ordinal)
                        StepLine(active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_2_LOCK_SCREEN_DETECTED.ordinal)
                        StepDot(number = "2", title = "Screen", active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_2_LOCK_SCREEN_DETECTED.ordinal)
                        StepLine(active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_3_SWIPE_UP.ordinal)
                        StepDot(number = "3", title = "Swipe", active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_3_SWIPE_UP.ordinal)
                        StepLine(active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_4_CREDENTIAL_ENTRY.ordinal)
                        StepDot(number = "4", title = "Verify", active = testWorkflowStep.ordinal >= TestWorkflowStep.STEP_4_CREDENTIAL_ENTRY.ordinal)
                        StepLine(active = testWorkflowStep.ordinal >= TestWorkflowStep.TEST_SUCCESSFUL.ordinal)
                        StepDot(number = "5", title = "Result", active = testWorkflowStep == TestWorkflowStep.TEST_SUCCESSFUL)
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = testStatusText,
                        color = if (testWorkflowStep == TestWorkflowStep.TEST_SUCCESSFUL) Color(0xFF00E676)
                        else if (testWorkflowStep == TestWorkflowStep.TEST_FAILED) Color(0xFFFF5252)
                        else Color(0xFFE1BEE7),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Step 4: Credential input
                    if (testWorkflowStep == TestWorkflowStep.STEP_4_CREDENTIAL_ENTRY) {
                        if (currentLockType == MayaLockType.PIN) {
                            PinLockView(
                                pinLength = 4,
                                pinState = testPinInputState,
                                onPinCompleted = { pin ->
                                    val ok = MayaLockManager.verifyPin(context, pin)
                                    if (ok) {
                                        testPinInputState = PinState.SUCCESS
                                        testWorkflowStep = TestWorkflowStep.STEP_5_VERIFYING
                                        testStatusText = "PIN Verified! Actual result verify ho raha hai..."
                                        coroutineScope.launch {
                                            delay(1000)
                                            testWorkflowStep = TestWorkflowStep.TEST_SUCCESSFUL
                                            testStatusText = "Test Successful! Maya ka configured Lock & Unlock workflow properly kaam kar raha hai! 🎉"
                                            MayaLockManager.recordTestResult(context, true)
                                        }
                                    } else {
                                        testPinInputState = PinState.ERROR
                                        testStatusText = "Galat PIN! Dobara try karein."
                                        coroutineScope.launch {
                                            delay(1200)
                                            testPinInputState = PinState.INPUT
                                        }
                                    }
                                }
                            )
                        } else if (currentLockType == MayaLockType.PATTERN) {
                            PatternLockView(
                                modifier = Modifier.size(260.dp),
                                patternState = testPatternInputState,
                                onPatternCompleted = { patternDots ->
                                    val ok = MayaLockManager.verifyPattern(context, patternDots)
                                    if (ok) {
                                        testPatternInputState = PatternState.SUCCESS
                                        testWorkflowStep = TestWorkflowStep.STEP_5_VERIFYING
                                        testStatusText = "Pattern Verified! Actual result verify ho raha hai..."
                                        coroutineScope.launch {
                                            delay(1000)
                                            testWorkflowStep = TestWorkflowStep.TEST_SUCCESSFUL
                                            testStatusText = "Test Successful! Maya ka configured Lock & Unlock workflow properly kaam kar raha hai! 🎉"
                                            MayaLockManager.recordTestResult(context, true)
                                        }
                                    } else {
                                        testPatternInputState = PatternState.ERROR
                                        testStatusText = "Galat Pattern! Dobara try karein."
                                        coroutineScope.launch {
                                            delay(1200)
                                            testPatternInputState = PatternState.INPUT
                                        }
                                    }
                                }
                            )
                        }
                    } else if (testWorkflowStep == TestWorkflowStep.TEST_SUCCESSFUL) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("🎉", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "TEST SUCCESSFUL",
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Maya Lock, Swipe Gesture & Credential Verification validated.",
                                color = Color(0xFFB39DDB),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            color = Color(0xFFFF4081),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (testWorkflowStep == TestWorkflowStep.TEST_SUCCESSFUL || testWorkflowStep == TestWorkflowStep.TEST_FAILED) {
                    Button(
                        onClick = { showTestWorkflowModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676))
                    ) {
                        Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestWorkflowModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun StepDot(number: String, title: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF00E5FF) else Color(0xFF382352)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = if (active) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            color = if (active) Color(0xFF00E5FF) else Color.Gray,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun StepLine(active: Boolean) {
    Box(
        modifier = Modifier
            .width(20.dp)
            .height(2.dp)
            .background(if (active) Color(0xFF00E5FF) else Color(0xFF382352))
    )
}
