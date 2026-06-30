package com.example.digitaluniversity.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

// Student bottom nav — no chat
@Composable
fun BottomNav(navController: NavController) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("student") },
            icon = { Icon(Icons.Outlined.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("schedule") },
            icon = { Icon(Icons.Outlined.CalendarMonth, null) },
            label = { Text("Schedule") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("profile") },
            icon = { Icon(Icons.Outlined.PersonOutline, null) },
            label = { Text("Profile") }
        )
    }
}

// Lecturer bottom nav — no chat
@Composable
fun LecturerBottomNav(navController: NavController) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("lecturer") },
            icon = { Icon(Icons.Outlined.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("lecturer_schedule") },
            icon = { Icon(Icons.Outlined.CalendarMonth, null) },
            label = { Text("Schedule") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("lecturer_students") },
            icon = { Icon(Icons.Outlined.Groups, null) },
            label = { Text("Students") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("profile") },
            icon = { Icon(Icons.Outlined.PersonOutline, null) },
            label = { Text("Profile") }
        )
    }
}