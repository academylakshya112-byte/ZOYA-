package com.example.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ZoyaForegroundService
import com.example.notification.CallAnnouncer
import com.example.service.GeminiVisionAnalyzer
import com.example.service.VisionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScannerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera states
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Scanner mode: "study" vs "general"
    var scannerMode by remember { mutableStateOf("study") }

    // Analysis states
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<VisionResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }
    var capturedPreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isAnalyzing = true
                errorMessage = null
                analysisResult = null
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        val input: InputStream? = context.contentResolver.openInputStream(uri)
                        BitmapFactory.decodeStream(input)
                    }
                    if (bitmap != null) {
                        capturedPreviewBitmap = bitmap
                        val result = GeminiVisionAnalyzer.analyzeImage(context, bitmap, scannerMode)
                        result.fold(
                            onSuccess = { res ->
                                analysisResult = res
                                // Auto speak brief intro if study mode
                                if (scannerMode == "study") {
                                    CallAnnouncer.speakText(res.spokenSummary)
                                    isSpeaking = true
                                }
                            },
                            onFailure = { err ->
                                errorMessage = err.localizedMessage ?: "Analysis failed"
                            }
                        )
                    }
                } catch (e: Exception) {
                    errorMessage = e.localizedMessage
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    // Animated Scanning Laser Line
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserYRatio by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0814))
    ) {
        if (hasCameraPermission) {
            // Live CameraX Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val executor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        imageCapture = capture

                        try {
                            cameraProvider.unbindAll()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            cameraControl = cam.cameraControl
                        } catch (e: Exception) {
                            Log.e("CameraScanner", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = {
                    // Re-bind when cameraSelector changes
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(it.surfaceProvider)
                        }
                        try {
                            cameraProvider.unbindAll()
                            val capture = imageCapture ?: ImageCapture.Builder().build()
                            val cam = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            cameraControl = cam.cameraControl
                            cam.cameraControl.enableTorch(isFlashOn)
                        } catch (e: Exception) {
                            Log.e("CameraScanner", "Update rebind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )

            // Sci-Fi HUD Overlay Canvas
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height

                val scanLeft = w * 0.08f
                val scanTop = h * 0.18f
                val scanRight = w * 0.92f
                val scanBottom = h * 0.72f
                val cornerLength = 40.dp.toPx()
                val cornerStroke = 4.dp.toPx()
                val hudColor = if (scannerMode == "study") Color(0xFF00E5FF) else Color(0xFFFF4081)

                // Draw 4 Corner Brackets
                // Top-Left
                drawLine(hudColor, Offset(scanLeft, scanTop), Offset(scanLeft + cornerLength, scanTop), strokeWidth = cornerStroke)
                drawLine(hudColor, Offset(scanLeft, scanTop), Offset(scanLeft, scanTop + cornerLength), strokeWidth = cornerStroke)

                // Top-Right
                drawLine(hudColor, Offset(scanRight, scanTop), Offset(scanRight - cornerLength, scanTop), strokeWidth = cornerStroke)
                drawLine(hudColor, Offset(scanRight, scanTop), Offset(scanRight, scanTop + cornerLength), strokeWidth = cornerStroke)

                // Bottom-Left
                drawLine(hudColor, Offset(scanLeft, scanBottom), Offset(scanLeft + cornerLength, scanBottom), strokeWidth = cornerStroke)
                drawLine(hudColor, Offset(scanLeft, scanBottom), Offset(scanLeft, scanBottom - cornerLength), strokeWidth = cornerStroke)

                // Bottom-Right
                drawLine(hudColor, Offset(scanRight, scanBottom), Offset(scanRight - cornerLength, scanBottom), strokeWidth = cornerStroke)
                drawLine(hudColor, Offset(scanRight, scanBottom), Offset(scanRight, scanBottom - cornerLength), strokeWidth = cornerStroke)

                // Laser Scanning Line
                if (!isAnalyzing && analysisResult == null) {
                    val laserY = scanTop + (scanBottom - scanTop) * laserYRatio
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, hudColor, Color.White, hudColor, Color.Transparent),
                            startX = scanLeft,
                            endX = scanRight
                        ),
                        start = Offset(scanLeft, laserY),
                        end = Offset(scanRight, laserY),
                        strokeWidth = 3.dp.toPx()
                    )
                }
            }
        } else {
            // Camera Permission Needed State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "To scan questions, study material, and analyze objects with Maya AI, please allow camera access.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Controls Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = {
                        CallAnnouncer.stopSpeaking()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Mode Selector Chips
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModeChip(
                        selected = scannerMode == "study",
                        title = "📚 Study Solver",
                        activeColor = Color(0xFF00E5FF),
                        onClick = { scannerMode = "study" }
                    )
                    ModeChip(
                        selected = scannerMode == "general",
                        title = "🔍 Vision AI",
                        activeColor = Color(0xFFFF4081),
                        onClick = { scannerMode = "general" }
                    )
                }

                // Flash & Flip Camera Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Flashlight
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier
                            .background(if (isFlashOn) Color(0xFFFFD54F) else Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }

                    // Flip Camera
                    IconButton(
                        onClick = {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Mode helper tag
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (scannerMode == "study") "💡 Align question, equation, or study book in frame" else "👁️ Point at any object, document, or scene to analyze",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Controls Bar (Shutter, Gallery, Screen Read)
        if (analysisResult == null && !isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Picker
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gallery", color = Color.White, fontSize = 11.sp)
                    }

                    // Big Futuristic Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (scannerMode == "study") Color(0xFF00E5FF) else Color(0xFFFF4081),
                                        Color(0xFF7C4DFF)
                                    )
                                )
                            )
                            .clickable {
                                val capture = imageCapture
                                if (capture != null) {
                                    isAnalyzing = true
                                    errorMessage = null
                                    analysisResult = null

                                    capture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                                coroutineScope.launch {
                                                    try {
                                                        val bitmap = imageProxyToBitmap(imageProxy)
                                                        imageProxy.close()
                                                        capturedPreviewBitmap = bitmap
                                                        val result = GeminiVisionAnalyzer.analyzeImage(
                                                            context,
                                                            bitmap,
                                                            scannerMode
                                                        )
                                                        result.fold(
                                                            onSuccess = { res ->
                                                                analysisResult = res
                                                                if (scannerMode == "study") {
                                                                    CallAnnouncer.speakText(res.spokenSummary)
                                                                    isSpeaking = true
                                                                }
                                                            },
                                                            onFailure = { err ->
                                                                errorMessage = err.localizedMessage
                                                            }
                                                        )
                                                    } catch (e: Exception) {
                                                        errorMessage = e.localizedMessage
                                                    } finally {
                                                        isAnalyzing = false
                                                    }
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isAnalyzing = false
                                                errorMessage = "Camera capture failed: ${exception.message}"
                                            }
                                        }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .border(3.dp, Color.White, CircleShape)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        )
                    }

                    // Screen OCR / Quick Ask
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            ZoyaForegroundService.activeService?.sendTextMessage("read screen")
                            Toast.makeText(context, "Analyzing screen text with Maya...", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = "Read Screen", tint = Color(0xFF00E5FF))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Screen", color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }

        // Loading Overlay
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        color = if (scannerMode == "study") Color(0xFF00E5FF) else Color(0xFFFF4081),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = if (scannerMode == "study") "🧠 Maya is solving and breaking down the concept..." else "👁️ Maya is analyzing visual details...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Preparing step-by-step master explanation",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Analysis Result Card (Scrollable Sheet)
        if (analysisResult != null) {
            val res = analysisResult!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(16.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF160E24)),
                    border = BorderStroke(1.dp, if (res.isStudy) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFFFF4081).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Header Bar of Result Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(if (res.isStudy) "🎓" else "🔍", fontSize = 24.sp)
                                Column {
                                    Text(
                                        text = if (res.isStudy) "Study Solution & Breakdown" else "Vision Analysis",
                                        color = if (res.isStudy) Color(0xFF00E5FF) else Color(0xFFFF4081),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "AI Verified by Maya",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Close / Scan Another
                            IconButton(
                                onClick = {
                                    CallAnnouncer.stopSpeaking()
                                    isSpeaking = false
                                    analysisResult = null
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Audio Voice Control Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                    contentDescription = "Voice Explanation",
                                    tint = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = if (isSpeaking) "Maya is speaking explanation..." else "Listen to voice explanation",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Button(
                                onClick = {
                                    if (isSpeaking) {
                                        CallAnnouncer.stopSpeaking()
                                        isSpeaking = false
                                    } else {
                                        CallAnnouncer.speakText(res.spokenSummary)
                                        isSpeaking = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSpeaking) Color(0xFFFF5252) else Color(0xFF00E5FF)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = if (isSpeaking) "Stop ⏹️" else "Play 🔊",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Scrollable Content Body
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Explanation Markdown Text formatted nicely
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = res.explanation,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons Row: Copy, Ask Maya Follow-up, Scan Next
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Copy Button
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Study Solution", res.explanation)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Solution copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", color = Color.White, fontSize = 13.sp)
                            }

                            // Ask Follow-up Button
                            Button(
                                onClick = {
                                    ZoyaForegroundService.activeService?.sendTextMessage("Explain this question more deeply: ${res.title}")
                                    Toast.makeText(context, "Sent to Maya voice session!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Ask Maya", tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ask Maya 💬", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Error Banner if analysis fails
        if (errorMessage != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { errorMessage = null }) {
                        Text("Dismiss", color = Color(0xFF00E5FF))
                    }
                }
            ) {
                Text(errorMessage ?: "Error occurred", color = Color.White)
            }
        }
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    title: String,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) activeColor else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.Black else Color.White.copy(alpha = 0.8f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 12.sp
        )
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    val rotationDegrees = image.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
