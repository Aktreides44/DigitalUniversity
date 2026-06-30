package com.example.digitaluniversity.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.courses.CourseViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class CourseAnalytics(
    val courseName: String,
    val courseCode: String,
    val studentCount: Int,
    val assignmentCount: Int,
    val submissionCount: Int,
    val averageGrade: Double?,
    val gradedCount: Int
)

@Composable
fun LecturerAnalyticsScreen(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val lecturerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val courses by vm.courses.observeAsState(emptyList())
    var analytics by remember { mutableStateOf<List<CourseAnalytics>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) { vm.loadLecturerCourses(lecturerId) }

    LaunchedEffect(courses) {
        if (courses.isEmpty()) { isLoading = false; return@LaunchedEffect }
        val result = mutableListOf<CourseAnalytics>()
        var pending = courses.size

        courses.forEach { course ->
            var students = 0; var assignments = 0
            var submissions = 0; var gradedCount = 0
            val grades = mutableListOf<Float>()
            var innerPending = 3

            fun tryBuild() {
                innerPending--
                if (innerPending <= 0) {
                    result.add(CourseAnalytics(
                        courseName = course.courseName,
                        courseCode = course.courseCode,
                        studentCount = students,
                        assignmentCount = assignments,
                        submissionCount = submissions,
                        averageGrade = if (grades.isEmpty()) null else grades.average(),
                        gradedCount = gradedCount
                    ))
                    pending--
                    if (pending <= 0) {
                        analytics = result.sortedBy { it.courseName }
                        isLoading = false
                    }
                }
            }

            db.collection("enrollments").whereEqualTo("courseId", course.courseId).get()
                .addOnSuccessListener { students = it.size(); tryBuild() }
                .addOnFailureListener { tryBuild() }

            db.collection("assignments").whereEqualTo("courseId", course.courseId).get()
                .addOnSuccessListener { assignments = it.size(); tryBuild() }
                .addOnFailureListener { tryBuild() }

            db.collection("submissions").whereEqualTo("courseId", course.courseId).get()
                .addOnSuccessListener { snap ->
                    submissions = snap.size()
                    snap.documents.forEach { doc ->
                        val g = doc.getString("grade")?.toFloatOrNull()
                        if (g != null) { grades.add(g); gradedCount++ }
                    }
                    tryBuild()
                }
                .addOnFailureListener { tryBuild() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgGray).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Outlined.ArrowBack, null)
                }
                Text("Teaching Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
        } else if (analytics.isEmpty()) {
            item { EmptyCard("No course data available yet") }
        } else {
            items(analytics) { ca ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Course title
                        Text(ca.courseName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(ca.courseCode, color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(12.dp))

                        // Stat row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniStat("Students", ca.studentCount.toString(), Color(0xFF7B39FD), Modifier.weight(1f))
                            MiniStat("Assignments", ca.assignmentCount.toString(), Color(0xFFE91E63), Modifier.weight(1f))
                            MiniStat("Submissions", ca.submissionCount.toString(), Color(0xFF009688), Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(12.dp))
                        Divider(color = Color(0xFFEEEEEE))
                        Spacer(Modifier.height(12.dp))

                        // Grade summary
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Average Grade", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    if (ca.averageGrade != null) String.format("%.2f / 5.0", ca.averageGrade)
                                    else "No grades yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = if (ca.averageGrade != null) PrimaryPurple else Color.Gray
                                )
                                Text("${ca.gradedCount} of ${ca.submissionCount} graded", color = Color.Gray, fontSize = 11.sp)
                            }

                            // Submission rate bar
                            if (ca.studentCount > 0 && ca.assignmentCount > 0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    val rate = (ca.submissionCount.toFloat() /
                                            (ca.studentCount * ca.assignmentCount).coerceAtLeast(1)) * 100
                                    Text("Submission rate", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        "${rate.toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            rate >= 75 -> Color(0xFF4CAF50)
                                            rate >= 40 -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(0.08f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(0.8f))
        }
    }
}