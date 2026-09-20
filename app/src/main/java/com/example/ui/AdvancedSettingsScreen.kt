package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }

    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFF100A1A), // Deep dark matching screenshot
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Advanced",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF100A1A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Gemini API Key Card (Exact match to Screenshot 4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1226))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Gemini API key",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔑", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            saveStatus = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("••••••••••••••••••••••••••••", color = Color.Gray, fontSize = 14.sp)
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.Lock,
                                    contentDescription = "Toggle visibility",
                                    tint = Color(0xFFB39DDB)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF6584),
                            unfocusedBorderColor = Color(0xFF38224D),
                            focusedContainerColor = Color(0xFF150D20),
                            unfocusedContainerColor = Color(0xFF150D20)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            prefs.edit().putString("api_key", apiKey.trim()).apply()
                            saveStatus = "API Key saved successfully!"
                            Toast.makeText(context, "API Key Saved!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5277) // Pink accent from screenshot
                        ),
                        shape = RoundedCornerShape(22.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text(
                            "Save key",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (saveStatus != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = saveStatus ?: "",
                            color = Color(0xFF00E676),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Get a free Gemini 2.0 / 2.5 key at Google AI Studio ↗",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
