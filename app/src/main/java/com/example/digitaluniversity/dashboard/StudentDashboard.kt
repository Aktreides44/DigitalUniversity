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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.courses.CourseViewModel
import com.example.digitaluniversity.courses.firstScheduleDisplay
import com.google.firebase.auth.FirebaseAuth

val PrimaryPurple = Color(0xFF7B39FD)
val BgGray = Color(0xFFF8F9FB)

@Composable
fun StudentDashboard(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {

    val courses by vm.courses.observeAsState(emptyList())
    val enrolledIds = remember { mutableStateOf(listOf<String>()) }

    val currentEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    // Load courses once
    LaunchedEffect(Unit) {
        vm.loadCourses()
    }

    LaunchedEffect(currentEmail) {
        if (currentEmail.isNotBlank()) {
            android.util.Log.d("DEBUG", "Fetching enrollments for: '$currentEmail'")
            vm.getEnrolledCourses(currentEmail) {
                android.util.Log.d("DEBUG", "Found enrolled IDs: $it")
                enrolledIds.value = it
            }
        }
    }
    // Load enrollments separately
    LaunchedEffect(currentEmail) {
        if (currentEmail.isNotBlank()) {
            vm.getEnrolledCourses(currentEmail) {
                enrolledIds.value = it
            }
        }
    }

    Scaffold(
        bottomBar = { BottomNav(navController) },
        containerColor = BgGray
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item { Spacer(Modifier.height(10.dp)) }

            item { Header() }

            // Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Welcome back 👋",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Manage your courses and learning",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Quick Nav
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {


                    NavCard(
                        label = "Schedule",
                        icon = Icons.Outlined.CalendarMonth,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("schedule") }
                    )

                    NavCard(
                        label = "Chat",
                        icon = Icons.Outlined.ChatBubbleOutline,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("chat") }
                    )

                    NavCard(
                        "Courses",
                        Icons.Outlined.MenuBook,
                        Modifier.weight(1f)
                    )

                    NavCard(
                        label = "Profile",
                        icon = Icons.Outlined.PersonOutline,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navController.navigate("profile")
                        }
                    )
                }
            }

            // MY COURSES
            item {
                SectionHeader("My Courses", "Already enrolled")
            }

            val myCourses = courses.filter {
                enrolledIds.value.contains(it.courseId)
            }

            if (myCourses.isEmpty()) {
                item {
                    Text(
                        "No enrolled courses yet",
                        color = Color.Gray
                    )
                }
            } else {
                items(myCourses) { course ->
                    CourseCard(
                        name = course.courseName,
                        code = course.courseCode,
                        lecturer = course.lecturerName,
                        schedule = course.firstScheduleDisplay(),
                        onClick = {
                            navController.navigate(
                                "course_detail/${course.courseId}"
                            )
                        }
                    )
                }
            }

            // AVAILABLE COURSES
            item {
                SectionHeader("Available Courses", "Tap to enroll")
            }

            val openCourses = courses.filter {
                !enrolledIds.value.contains(it.courseId)
            }

            if (openCourses.isEmpty()) {
                item {
                    Text("No courses available", color = Color.Gray)
                }
            } else {
                items(openCourses) { course ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Text(course.courseName, fontWeight = FontWeight.Bold)
                            Text(course.courseCode, color = Color.Gray, fontSize = 12.sp)
                            Text(course.lecturerName, color = Color.Gray, fontSize = 12.sp)

                            Text(
                                course.firstScheduleDisplay(),
                                color = PrimaryPurple,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    vm.enrollStudent(currentEmail, course.courseId) {
                                        vm.getEnrolledCourses(currentEmail) {
                                            enrolledIds.value = it
                                        }
                                    }
                                }
                            ) {
                                Text("Enroll")
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
// --- Supporting Components ---

@Composable
fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Text("DU", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Student", fontWeight = FontWeight.Bold)
                Text("Digital University", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Icon(Icons.Outlined.Notifications, contentDescription = null)
    }
}

@Composable
fun NavCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = PrimaryPurple)
            Spacer(Modifier.height(5.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CourseCard(name: String, code: String, lecturer: String, schedule: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(name, fontWeight = FontWeight.Bold)
            Text(code, color = Color.Gray, fontSize = 12.sp)
            Text(lecturer, color = Color.Gray, fontSize = 12.sp)
            Text(schedule, color = PrimaryPurple, fontSize = 12.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, fontSize = 13.sp, color = Color.Gray)
    }
}

