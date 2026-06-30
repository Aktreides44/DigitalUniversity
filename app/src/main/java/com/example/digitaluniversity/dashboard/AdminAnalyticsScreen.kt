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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

data class AdminStats(
    val totalStudents: Int = 0,
    val totalLecturers: Int = 0,
    val totalCourses: Int = 0,
    val totalEnrollments: Int = 0,
    val totalAssignments: Int = 0,
    val totalSubmissions: Int = 0,
    val courseEnrollmentCounts: List<Pair<String, Int>> = emptyList()
)

@Composable
fun AdminAnalyticsScreen(navController: NavController) {
    var stats by remember { mutableStateOf(AdminStats()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        // Load all in parallel using a simple counter
        var pending = 6
        var students = 0; var lecturers = 0; var courses = 0
        var enrollments = 0; var assignments = 0; var submissions = 0
        val courseNames = mutableMapOf<String, String>()
        val enrollCounts = mutableMapOf<String, Int>()

        fun tryFinish() {
            pending--
            if (pending <= 0) {
                val sorted = enrollCounts.entries
                    .sortedByDescending { it.value }
                    .map { (id, count) -> Pair(courseNames[id] ?: id, count) }
                stats = AdminStats(students, lecturers, courses, enrollments, assignments, submissions, sorted)
                isLoading = false
            }
        }

        db.collection("users").get().addOnSuccessListener { snap ->
            snap.documents.forEach {
                when (it.getString("role")?.lowercase()) {
                    "student" -> students++
                    "lecturer" -> lecturers++
                }
            }
            tryFinish()
        }.addOnFailureListener { tryFinish() }

        db.collection("courses").get().addOnSuccessListener { snap ->
            courses = snap.size()
            snap.documents.forEach { doc ->
                val id = doc.getString("courseId") ?: doc.id
                courseNames[id] = doc.getString("courseName") ?: id
                enrollCounts[id] = 0
            }
            tryFinish()
        }.addOnFailureListener { tryFinish() }

        db.collection("enrollments").get().addOnSuccessListener { snap ->
            enrollments = snap.size()
            snap.documents.forEach { doc ->
                val cId = doc.getString("courseId") ?: return@forEach
                enrollCounts[cId] = (enrollCounts[cId] ?: 0) + 1
            }
            tryFinish()
        }.addOnFailureListener { tryFinish() }

        db.collection("assignments").get().addOnSuccessListener { snap ->
            assignments = snap.size(); tryFinish()
        }.addOnFailureListener { tryFinish() }

        db.collection("submissions").get().addOnSuccessListener { snap ->
            submissions = snap.size(); tryFinish()
        }.addOnFailureListener { tryFinish() }

        // dummy 6th to keep counter balanced
        tryFinish()
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
                Text("Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
        } else {
            // Stat cards grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Students", stats.totalStudents.toString(),
                            Icons.Outlined.School, Color(0xFF7B39FD), Modifier.weight(1f))
                        StatCard("Lecturers", stats.totalLecturers.toString(),
                            Icons.Outlined.Person, Color(0xFF2196F3), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Courses", stats.totalCourses.toString(),
                            Icons.Outlined.MenuBook, Color(0xFF4CAF50), Modifier.weight(1f))
                        StatCard("Enrollments", stats.totalEnrollments.toString(),
                            Icons.Outlined.Groups, Color(0xFFFF9800), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Assignments", stats.totalAssignments.toString(),
                            Icons.Outlined.Assignment, Color(0xFFE91E63), Modifier.weight(1f))
                        StatCard("Submissions", stats.totalSubmissions.toString(),
                            Icons.Outlined.UploadFile, Color(0xFF009688), Modifier.weight(1f))
                    }
                }
            }

            // Most popular courses
            item {
                SectionHeader("Course Popularity", "Ranked by enrollment count")
            }

            if (stats.courseEnrollmentCounts.isEmpty()) {
                item { EmptyCard("No enrollment data yet") }
            } else {
                items(stats.courseEnrollmentCounts.take(10)) { (name, count) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold)
                                Text("$count students enrolled", color = Color.Gray, fontSize = 12.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryPurple
                            ) {
                                Text(
                                    count.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, color = Color.Gray, fontSize = 12.sp)
        }
    }
}