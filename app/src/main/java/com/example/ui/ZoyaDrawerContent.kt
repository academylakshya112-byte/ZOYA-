package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class DrawerNavDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : DrawerNavDestination("home", "Home", Icons.Default.Home)
    object MayaHome : DrawerNavDestination("maya_home", "Maya Home", Icons.Default.LocationOn)
    object MayaLock : DrawerNavDestination("maya_lock", "Lock & Security 🔐", Icons.Default.Security)
    object Persona : DrawerNavDestination("persona", "Persona Mode 🎭", Icons.Default.Face)
    object Settings : DrawerNavDestination("settings", "Settings", Icons.Default.Settings)
    object Documents : DrawerNavDestination("documents", "Documents", Icons.Default.FormatListBulleted)
    object StudyWhiteboard : DrawerNavDestination("study", "Study / Whiteboard", Icons.Default.Edit)
    object Permissions : DrawerNavDestination("permissions", "Permissions", Icons.Default.Lock)
    object About : DrawerNavDestination("about", "About", Icons.Default.Info)
    object PrivacyPolicy : DrawerNavDestination("privacy", "Privacy Policy", Icons.Default.Person)
}

@Composable
fun ZoyaDrawerContent(
    currentRoute: String,
    onNavigate: (DrawerNavDestination) -> Unit
) {
    val items = listOf(
        DrawerNavDestination.Home,
        DrawerNavDestination.MayaHome,
        DrawerNavDestination.MayaLock,
        DrawerNavDestination.Persona,
        DrawerNavDestination.Settings,
        DrawerNavDestination.Documents,
        DrawerNavDestination.StudyWhiteboard,
        DrawerNavDestination.Permissions,
        DrawerNavDestination.About,
        DrawerNavDestination.PrivacyPolicy
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xFF160D20)) // Dark purple matching user's screenshot
            .padding(vertical = 24.dp, horizontal = 16.dp)
    ) {
        // Top Header
        Column(modifier = Modifier.padding(start = 8.dp, bottom = 20.dp, top = 16.dp)) {
            // Pink Heart Icon
            Text(
                text = "💖",
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            // Maya Title
            Text(
                text = "Maya",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            // Subtitle
            Text(
                text = "by The Shadow X Rahul AI",
                color = Color(0xFFB39DDB),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Menu Items
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items.forEach { dest ->
                val isSelected = currentRoute == dest.route

                val backgroundColor = if (isSelected) {
                    Color(0xFF381E36) // Highlighted soft pink/purple pill
                } else {
                    Color.Transparent
                }

                val contentColor = if (isSelected) {
                    Color(0xFFFF70A6) // Pink text & icon when selected
                } else {
                    Color(0xFFE1BEE7) // Soft lavender when unselected
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(backgroundColor)
                        .clickable { onNavigate(dest) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = dest.icon,
                        contentDescription = dest.title,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = dest.title,
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
