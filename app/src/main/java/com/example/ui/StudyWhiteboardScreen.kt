package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ZoyaForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyWhiteboardScreen(onNavigateBack: () -> Unit) {
    var whiteboardNotes by remember { mutableStateOf("") }
    var promptQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFF140D1F),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Study / Whiteboard", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Interactive assistant canvas & study hub", color = Color(0xFFB39DDB), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { whiteboardNotes = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFFFF6584))
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
                .padding(16.dp)
        ) {
            // Whiteboard Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F132B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6584).copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📝 Canvas & Study Notes", color = Color(0xFFFF70A6), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Markdown / Text", color = Color.Gray, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextField(
                        value = whiteboardNotes,
                        onValueChange = { whiteboardNotes = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = {
                            Text(
                                "Use this whiteboard to brainstorm, prepare formulas, take live study notes, or prepare questions for Maya...",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Query input box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptQuery,
                    onValueChange = { promptQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Maya to explain a concept...", color = Color.Gray, fontSize = 13.sp) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF6584),
                        unfocusedBorderColor = Color(0xFF4A2040)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (promptQuery.isNotBlank()) {
                            ZoyaForegroundService.activeService?.sendTextMessage("Study Query: $promptQuery")
                            if (whiteboardNotes.isNotBlank()) {
                                whiteboardNotes += "\n\nQ: $promptQuery"
                            } else {
                                whiteboardNotes = "Q: $promptQuery"
                            }
                            promptQuery = ""
                        }
                    },
                    containerColor = Color(0xFFFF6584),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
