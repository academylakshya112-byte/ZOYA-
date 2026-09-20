package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.memory.MemoryManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var facts by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var skills by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    fun refresh() {
        facts = MemoryManager.getAllFacts(context)
        skills = MemoryManager.getAllSkills(context)
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        containerColor = Color(0xFF140D1F),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Documents & Memories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("AI facts, rules & long-term storage", color = Color(0xFFB39DDB), fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1F132B))
            )
        }
    ) { paddingValues ->
        if (facts.isEmpty() && skills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📑", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No documents or memories yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Say 'Remember that my car number is 1234' or 'Ye yaad rakhna' to teach Maya facts.",
                        color = Color(0xFFB0A5C4),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (facts.isNotEmpty()) {
                    item {
                        Text("SAVED FACTS & USER DATA", color = Color(0xFFFF70A6), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    items(facts, key = { "fact_${it.first}" }) { (key, value) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF261836)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6584).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Note, contentDescription = null, tint = Color(0xFFFF70A6), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = key.uppercase(),
                                        color = Color(0xFFFF70A6),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = value,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        MemoryManager.forgetMemory(context, key)
                                        refresh()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }

                if (skills.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("LEARNED SKILLS & TRIGGERS", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    items(skills, key = { "skill_${it.first}" }) { (trigger, action) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E295D)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "WHEN USER SAYS: '$trigger'",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = action,
                                        color = Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        MemoryManager.forgetMemory(context, trigger)
                                        refresh()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
