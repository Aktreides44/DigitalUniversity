package com.example.digitaluniversity.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.courses.CourseViewModel
import com.example.digitaluniversity.courses.firstScheduleDisplay
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LecturerDashboard(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val lecturerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val courses by vm.courses.observeAsState(emptyList())

    LaunchedEffect(Unit) { vm.loadLecturerCourses(lecturerId) }

    Scaffold(
        bottomBar = { BottomNav(navController) },
        containerColor = BgGray
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(10.dp)) }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(45.dp).clip(CircleShape).background(PrimaryPurple),
                            contentAlignment = Alignment.Center
                        ) { Text("DU", color = Color.White, fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Lecturer", fontWeight = FontWeight.Bold)
                            Text("Digital University", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                    Icon(Icons.Outlined.Notifications, null)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Teaching Dashboard 👨‍🏫", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Manage your assigned courses", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NavCard("Schedule", Icons.Outlined.CalendarMonth, Modifier.weight(1f),
                        onClick = { navController.navigate("lecturer_schedule") })
                    NavCard("Students", Icons.Outlined.Groups, Modifier.weight(1f),
                        onClick = { navController.navigate("lecturer_students") })  // ← wired
                    NavCard("Profile", Icons.Outlined.PersonOutline, Modifier.weight(1f),
                        onClick = { navController.navigate("profile") })
                    NavCard("Analytics", Icons.Outlined.BarChart, Modifier.weight(1f),
                        onClick = { navController.navigate("lecturer_analytics") }) // ← wired
                }
            }

            item { SectionHeader("Assigned Courses", "Courses you teach") }

            if (courses.isEmpty()) {
                item { Text("No assigned courses", color = Color.Gray) }
            } else {
                items(courses) { course ->
                    CourseCard(
                        name = course.courseName,
                        code = course.courseCode,
                        lecturer = course.lecturerName,
                        schedule = course.firstScheduleDisplay(),
                        onClick = { navController.navigate("course_detail/${course.courseId}") }
                    )
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}