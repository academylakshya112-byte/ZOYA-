package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF140D1F),
        topBar = {
            TopAppBar(
                title = { Text("About Maya AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F132B))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("💖", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Maya", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("by The Shadow X Rahul AI", color = Color(0xFFB39DDB), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261836))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Architecture & Intelligence", color = Color(0xFFFF70A6), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Engine: Gemini 2.0 Multimodal Live Bidirectional WebSocket\n" +
                                "• Full Hands-Free Voice Navigation\n" +
                                "• Smart Natural Hindi, Hinglish & English NLU Engine\n" +
                                "• WhatsApp Automated Message & Calling Assistant\n" +
                                "• Notification Listener with Autonomous Safe Mode\n" +
                                "• On-Device Room Persistent Memory Matrix",
                        color = Color(0xFFE1BEE7),
                        fontSize = 13.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF140D1F),
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F132B))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("🛡️ Your Privacy is Protected", color = Color(0xFFFF70A6), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "1. On-Device Memory\n" +
                        "All personal memories, contacts cache, and preference records are stored 100% locally on your device inside SQLite/Room database.\n\n" +
                        "2. Notification Privacy\n" +
                        "Notification data intercepted by Maya is processed strictly in-memory. In Privacy Mode, sender names and message previews are hidden from spoken output.\n\n" +
                        "3. Autonomous Safety Boundary\n" +
                        "Maya will NEVER autonomously send payments, money commitments (₹), passwords, OTPs, or sensitive banking information without explicit verbal confirmation.\n\n" +
                        "4. Direct AI Studio Secure Link\n" +
                        "Your API key connects directly to Google AI Studio Gemini Live endpoints without passing through any third-party intermediate servers.",
                color = Color(0xFFE1BEE7),
                fontSize = 13.sp,
                lineHeight = 22.sp
            )
        }
    }
}
