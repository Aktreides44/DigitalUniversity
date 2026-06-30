package com.example.digitaluniversity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.courses.Course
import com.example.digitaluniversity.courses.CourseSchedule
import com.example.digitaluniversity.courses.CourseViewModel
import com.example.digitaluniversity.dashboard.BgGray
import com.example.digitaluniversity.dashboard.PrimaryPurple
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val courses by vm.courses.observeAsState(emptyList())
    val enrolledIds = remember { mutableStateOf(listOf<String>()) }
    val currentEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

    LaunchedEffect(Unit) { vm.loadCourses() }

    LaunchedEffect(currentEmail) {
        if (currentEmail.isNotBlank()) {
            vm.getEnrolledCourses(currentEmail) {
                enrolledIds.value = it
            }
        }
    }

    // Only enrolled courses
    val myCourses = courses.filter { enrolledIds.value.contains(it.courseId) }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var selectedDay by remember { mutableStateOf("Monday") }

    // Build map: day -> list of (course, schedule) sorted by start time
    val scheduleMap: Map<String, List<Pair<Course, CourseSchedule>>> = days.associateWith { day ->
        myCourses.flatMap { course ->
            course.schedules
                .filter { it.day == day }
                .map { schedule -> course to schedule }
        }.sortedBy { it.second.startTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Schedule") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(padding)
        ) {

            // ── Day tabs ──
            ScrollableTabRow(
                selectedTabIndex = days.indexOf(selectedDay),
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                edgePadding = 0.dp
            ) {
                days.forEach { day ->
                    Tab(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        text = {
                            Text(
                                day.take(3).uppercase(),
                                fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ── Classes for selected day ──
            val sessionsToday = scheduleMap[selectedDay] ?: emptyList()

            if (sessionsToday.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎉", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No classes on $selectedDay",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "${sessionsToday.size} class${if (sessionsToday.size > 1) "es" else ""} today",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    sessionsToday.forEachIndexed { index, (course, schedule) ->
                        item {
                            TimetableRow(index = index, course = course, schedule = schedule)
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}