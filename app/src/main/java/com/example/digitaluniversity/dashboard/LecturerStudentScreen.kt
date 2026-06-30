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

data class StudentInCourse(
    val email: String,
    val courseName: String,
    val courseId: String
)

@Composable
fun LecturerStudentsScreen(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val lecturerId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val courses by vm.courses.observeAsState(emptyList())
    var studentsByCourse by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        vm.loadLecturerCourses(lecturerId)
    }

    LaunchedEffect(courses) {
        if (courses.isEmpty()) { isLoading = false; return@LaunchedEffect }
        val result = mutableMapOf<String, List<String>>()
        var pending = courses.size
        courses.forEach { course ->
            db.collection("enrollments")
                .whereEqualTo("courseId", course.courseId)
                .get()
                .addOnSuccessListener { snap ->
                    result[course.courseId] = snap.documents.mapNotNull { it.getString("studentEmail") }
                    pending--
                    if (pending <= 0) {
                        studentsByCourse = result
                        isLoading = false
                    }
                }
                .addOnFailureListener {
                    pending--
                    if (pending <= 0) { studentsByCourse = result; isLoading = false }
                }
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
                Text("My Students", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
        } else {
            courses.forEach { course ->
                val students = studentsByCourse[course.courseId] ?: emptyList()
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(PrimaryPurple)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(course.courseName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(course.courseCode, color = Color.White.copy(0.8f), fontSize = 12.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(0.2f)
                            ) {
                                Text(
                                    "${students.size} students",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (students.isEmpty()) {
                    item { EmptyCard("No students enrolled in ${course.courseName}") }
                } else {
                    items(students) { email ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PrimaryPurple.copy(0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        email.take(1).uppercase(),
                                        color = PrimaryPurple,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(email, fontSize = 14.sp)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(4.dp)) }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}