package com.example.digitaluniversity.navigation

import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.digitaluniversity.LecturerScheduleScreen
import com.example.digitaluniversity.LoginScreen
import com.example.digitaluniversity.RegisterScreen
import com.example.digitaluniversity.ScheduleScreen
import com.example.digitaluniversity.dashboard.assignment.AssignmentScreen
import com.example.digitaluniversity.auth.UserViewModel
import com.example.digitaluniversity.chat.ChatRoomScreen
import com.example.digitaluniversity.dashboard.*
import com.example.digitaluniversity.dashboard.assignment.SubmissionScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val userVm: UserViewModel = viewModel()
    val role by userVm.role.observeAsState("")
    val displayName by userVm.displayName.observeAsState("")

    LaunchedEffect(Unit) {
        if (FirebaseAuth.getInstance().currentUser != null) {
            userVm.loadCurrentUser()
        }
    }

    NavHost(navController = navController, startDestination = "login") {

        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }

        composable("student") {
            LaunchedEffect(Unit) { userVm.loadCurrentUser() }
            StudentDashboard(navController = navController)
        }
        composable("lecturer") {
            LaunchedEffect(Unit) { userVm.loadCurrentUser() }
            LecturerDashboard(navController)
        }
        composable("admin") {
            LaunchedEffect(Unit) { userVm.loadCurrentUser() }
            AdminDashboard(navController)
        }

        composable("lecturer_schedule") { LecturerScheduleScreen(navController) }
        composable("schedule") { ScheduleScreen(navController) }
        composable("create_course") { AdminCreateCourseScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("users") { UsersScreen(navController) }

        // ── NEW SCREENS ──
        composable("admin_analytics") { AdminAnalyticsScreen(navController) }
        composable("lecturer_students") { LecturerStudentsScreen(navController) }
        composable("lecturer_analytics") { LecturerAnalyticsScreen(navController) }

        composable("course_detail/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            CourseDetailScreen(courseId = courseId, navController = navController)
        }

        composable("assignments/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            AssignmentScreen(courseId = courseId, navController = navController, role = role)
        }

        composable("submission/{assignmentId}/{courseId}") { backStackEntry ->
            val assignmentId = backStackEntry.arguments?.getString("assignmentId") ?: ""
            val courseId = backStackEntry.arguments?.getString("courseId") ?: ""
            SubmissionScreen(
                assignmentId = assignmentId,
                courseId = courseId,
                role = role,
                studentName = displayName,
                navController = navController
            )
        }

        composable("chat_room/{chatId}/{chatTitle}/{role}/{senderName}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatTitle = backStackEntry.arguments?.getString("chatTitle") ?: "Chat"
            val chatRole = backStackEntry.arguments?.getString("role") ?: role
            val senderName = backStackEntry.arguments?.getString("senderName") ?: displayName
            ChatRoomScreen(
                chatId = chatId,
                chatTitle = chatTitle,
                currentRole = chatRole,
                currentName = senderName,
                navController = navController
            )
        }
    }
}