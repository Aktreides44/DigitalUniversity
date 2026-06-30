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
import com.example.digitaluniversity.courses.firstScheduleDisplay
import com.google.firebase.firestore.FirebaseFirestore



@Composable
fun AdminDashboard(
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val courses by vm.courses.observeAsState(emptyList())
    var courseToDelete by remember { mutableStateOf<com.example.digitaluniversity.courses.Course?>(null) }
    var courseToEdit by remember { mutableStateOf<com.example.digitaluniversity.courses.Course?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadCourses() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_course") },
                containerColor = PrimaryPurple
            ) { Text("+", color = Color.White, fontSize = 20.sp) }
        },
        bottomBar = { AdminBottomNav(navController) },
        containerColor = BgGray,
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = { TextButton(onClick = { snackbarMessage = null }) { Text("OK") } }
                ) { Text(msg) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { Spacer(Modifier.height(10.dp)) }
            item { AdminHeader() }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Welcome Admin 👑", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text("Manage courses, lecturers & students", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NavCard("Create", Icons.Outlined.Add, Modifier.weight(1f),
                        onClick = { navController.navigate("create_course") })
                    NavCard("Users", Icons.Outlined.PersonOutline, Modifier.weight(1f),
                        onClick = { navController.navigate("users") })
                    NavCard("Profile", Icons.Outlined.PersonOutline, Modifier.weight(1f),
                        onClick = { navController.navigate("profile") })
                    NavCard("Analytics", Icons.Outlined.BarChart, Modifier.weight(1f),
                        onClick = { navController.navigate("admin_analytics") })
                }
            }

            item { SectionHeader("All Courses", "Tap to view · Delete icon to remove") }

            if (courses.isEmpty()) {
                item { EmptyCard("No courses created yet") }
            }

            items(courses) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(Color.White),
                    onClick = { navController.navigate("course_detail/${course.courseId}") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(course.courseName, fontWeight = FontWeight.Bold)
                            Text(course.courseCode, color = Color.Gray, fontSize = 12.sp)
                            Text(course.lecturerName, color = Color.Gray, fontSize = 12.sp)
                            Text(course.firstScheduleDisplay(), color = PrimaryPurple, fontSize = 12.sp)
                        }
                        Row {
                            IconButton(
                                onClick = { courseToEdit = course }
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Edit",
                                    tint = PrimaryPurple
                                )
                            }

                            IconButton(
                                onClick = { courseToDelete = course }
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(0.7f)
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // ── CASCADE DELETE COURSE DIALOG ──
    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) courseToDelete = null },
            title = { Text("Delete Course") },
            text = {
                Text(
                    "Delete \"${course.courseName}\"?\n\n" +
                            "This will also delete all enrollments, assignments, " +
                            "submissions and chat messages for this course."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        cascadeDeleteCourse(
                            courseId = course.courseId,
                            onDone = {
                                vm.loadCourses()
                                courseToDelete = null
                                isDeleting = false
                                snackbarMessage = "Course deleted"
                            },
                            onError = { err ->
                                isDeleting = false
                                snackbarMessage = "Error: $err"
                            }
                        )
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete Everything")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isDeleting) courseToDelete = null }) { Text("Cancel") }
            }
        )
    }
    courseToEdit?.let { course ->

        var courseName by remember { mutableStateOf(course.courseName) }
        var courseCode by remember { mutableStateOf(course.courseCode) }
        var description by remember { mutableStateOf(course.description) }
        var credits by remember { mutableStateOf(course.credits.toString()) }

        AlertDialog(
            onDismissRequest = { courseToEdit = null },

            title = {
                Text("Edit Course")
            },

            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Course Name") }
                    )

                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Course Code") }
                    )

                    OutlinedTextField(
                        value = credits,
                        onValueChange = { credits = it },
                        label = { Text("Credits") }
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        minLines = 3
                    )
                }
            },

            confirmButton = {
                Button(
                    onClick = {

                        val updatedCourse = course.copy(
                            courseName = courseName,
                            courseCode = courseCode,
                            description = description,
                            credits = credits.toIntOrNull() ?: 0
                        )

                        vm.updateCourse(
                            updatedCourse
                        ) {
                            vm.loadCourses()
                            courseToEdit = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },

            dismissButton = {
                TextButton(
                    onClick = { courseToEdit = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── CASCADE DELETE: course + all related data ──
fun cascadeDeleteCourse(
    courseId: String,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()
    var pendingOps = 0
    var completed = 0
    var failed = false

    fun tryFinish() {
        completed++
        if (completed >= pendingOps && !failed) onDone()
    }

    // Delete the course document
    batch.delete(db.collection("courses").document(courseId))

    // Delete enrollments
    pendingOps++
    db.collection("enrollments").whereEqualTo("courseId", courseId).get()
        .addOnSuccessListener { snap ->
            snap.documents.forEach { batch.delete(it.reference) }
            // Delete assignments + their submissions
            db.collection("assignments").whereEqualTo("courseId", courseId).get()
                .addOnSuccessListener { aSnap ->
                    aSnap.documents.forEach { aDoc ->
                        batch.delete(aDoc.reference)
                        // submissions per assignment
                        db.collection("submissions")
                            .whereEqualTo("assignmentId", aDoc.id).get()
                            .addOnSuccessListener { sSnap ->
                                sSnap.documents.forEach { batch.delete(it.reference) }
                            }
                    }
                    // Delete chatMeta + messages
                    db.collection("chatMeta")
                        .whereEqualTo("courseId", courseId).get()
                        .addOnSuccessListener { cSnap ->
                            cSnap.documents.forEach { batch.delete(it.reference) }
                            // Commit everything
                            batch.commit()
                                .addOnSuccessListener { tryFinish() }
                                .addOnFailureListener { onError(it.message ?: "Batch failed") }
                        }
                        .addOnFailureListener { onError(it.message ?: "Failed") }
                }
                .addOnFailureListener { onError(it.message ?: "Failed") }
        }
        .addOnFailureListener { if (!failed) { failed = true; onError(it.message ?: "Failed") } }
}

@Composable
fun AdminHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .background(color = PrimaryPurple, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("DU", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Admin", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Digital University", color = Color.Gray, fontSize = 12.sp)
            }
        }
        Icon(Icons.Outlined.Notifications, contentDescription = null)
    }
}

// Admin has no chat in bottom nav
@Composable
fun AdminBottomNav(navController: NavController) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = true,
            onClick = { navController.navigate("admin") },
            icon = { Icon(Icons.Outlined.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("users") },
            icon = { Icon(Icons.Outlined.PersonOutline, null) },
            label = { Text("Users") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("admin_analytics") },
            icon = { Icon(Icons.Outlined.BarChart, null) },
            label = { Text("Analytics") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("profile") },
            icon = { Icon(Icons.Outlined.PersonOutline, null) },
            label = { Text("Profile") }
        )
    }
}
