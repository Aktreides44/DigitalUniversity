package com.example.digitaluniversity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
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
fun LecturerScheduleScreen(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val courses by vm.courses.observeAsState(emptyList())
    val lecturerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(Unit) {
        vm.loadLecturerCourses(lecturerId)
    }

    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var selectedDay by remember { mutableStateOf("Monday") }

    // Build map: day -> list of (course, schedule)
    val scheduleMap: Map<String, List<Pair<Course, CourseSchedule>>> = days.associateWith { day ->
        courses.flatMap { course ->
            course.schedules
                .filter { it.day == day }
                .map { schedule -> course to schedule }
        }.sortedBy { it.second.startTime }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Teaching Schedule") },
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

            // ── Day tabs (scrollable) ──
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
                                day.take(3).uppercase(), // Mon, Tue, etc.
                                fontWeight = if (selectedDay == day) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ── Timetable for selected day ──
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
                            "No classes on ${selectedDay}",
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
                            TimetableRow(
                                index = index,
                                course = course,
                                schedule = schedule
                            )
                        }
                    }

                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun TimetableRow(
    index: Int,
    course: Course,
    schedule: CourseSchedule
) {
    val accent = listOf(
        Color(0xFF7B39FD), // purple
        Color(0xFF2196F3), // blue
        Color(0xFF4CAF50), // green
        Color(0xFFFF9800), // orange
        Color(0xFFE91E63), // pink
    )[index % 5]

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // ── Time column ──
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                schedule.startTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                schedule.endTime,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }

        Spacer(Modifier.width(12.dp))

        // ── Colored left bar ──
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent)
        )

        Spacer(Modifier.width(12.dp))

        // ── Class card ──
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color dot
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        course.courseName.take(2).uppercase(),
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        course.courseName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        course.courseCode,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(label = "📍 ${schedule.room}", color = accent)
                        Chip(label = "⏱ ${schedule.startTime}–${schedule.endTime}", color = accent)
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}