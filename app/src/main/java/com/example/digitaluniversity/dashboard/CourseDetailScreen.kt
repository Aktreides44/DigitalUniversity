package com.example.digitaluniversity.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Group
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
import com.example.digitaluniversity.chat.ChatHub
import com.example.digitaluniversity.chat.ChatViewModel
import com.example.digitaluniversity.courses.CourseViewModel
import com.example.digitaluniversity.dashboard.assignment.AssignmentViewModel
import com.example.digitaluniversity.dashboard.assignment.CreateAssignmentDialog
import com.example.digitaluniversity.dashboard.assignment.SubmissionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalUriHandler


@Composable
fun CourseDetailScreen(
    courseId: String,
    navController: NavController,
    vm: CourseViewModel = viewModel()
) {
    val course by vm.selectedCourse.observeAsState()
    val students by vm.enrolledStudents.observeAsState(emptyList())

    var currentRole by remember { mutableStateOf("") }
    var currentName by remember { mutableStateOf("") }
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val assignmentVm: AssignmentViewModel = viewModel()
    val assignments by assignmentVm.assignments.observeAsState(emptyList())

    val submissionVm: SubmissionViewModel = viewModel()

    // Tab state — students get Grades tab, lecturers/admin do not
    // Tabs: About | Assignments | Chat | Grades (student only) | Students (lecturer only)
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAssignmentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) {
        vm.loadCourse(courseId)
        vm.loadStudentsInCourse(courseId)
        assignmentVm.loadAssignments(courseId)

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUid)
            .get()
            .addOnSuccessListener { doc ->
                currentRole = (doc.getString("role") ?: "").trim().lowercase()
                currentName = doc.getString("name")
                    ?: FirebaseAuth.getInstance().currentUser?.displayName
                            ?: "User"
            }
    }

    // Load my submissions when role is known (for grades tab)
    LaunchedEffect(currentRole, courseId) {
        if (currentRole == "student") {
            submissionVm.loadMySubmissionsForCourse(courseId)
        }
    }

    val isStudent = currentRole == "student"
    val isLecturer = currentRole == "lecturer"
    val isAdmin = currentRole == "admin"

    val tabs = buildList {
        add("About")

        if (!isAdmin) {
            add("Assignments")
        }

        add("Chat")

        if (isStudent) add("Grades")
        if (isLecturer || isAdmin) add("Students")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
    ) {

        // ── COURSE HEADER ──
        Card(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryPurple),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = course?.courseName ?: "Loading...",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(course?.courseCode ?: "", color = Color.White.copy(0.8f))
                Text("Lecturer: ${course?.lecturerName ?: "-"}", color = Color.White.copy(0.8f))
                val firstSession = course?.schedules?.firstOrNull()
                Text(
                    firstSession?.let { "${it.day} ${it.startTime} - ${it.endTime}" } ?: "No schedule",
                    color = Color.White.copy(0.8f)
                )
                Text("Room: ${firstSession?.room ?: "-"}", color = Color.White.copy(0.8f))
            }
        }

        // ── TABS ──
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = PrimaryPurple,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // ── TAB CONTENT ──
        when (tabs.getOrNull(selectedTab)) {

            "About" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("About Course", "Description & overview")
                        Spacer(Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = course?.description ?: "No description available",
                                modifier = Modifier.padding(16.dp),
                                color = Color.DarkGray
                            )
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }

            "Assignments" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader("Assignments", "Upcoming coursework")
                            if (isLecturer) {
                                Button(
                                    onClick = { showAssignmentDialog = true }
                                ) {
                                    Text("+ Add")
                                }
                            }
                        }
                    }

                    if (assignments.isEmpty()) {
                        item { EmptyCard("No assignments uploaded yet") }
                    } else {
                        items(assignments) { a ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(Color.White)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(a.title, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(a.description, color = Color.DarkGray, fontSize = 13.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Due: ${a.dueDate}", color = PrimaryPurple, fontSize = 13.sp)

                                    val uriHandler = LocalUriHandler.current

                                    if (a.attachmentUrl.isNotBlank()) {
                                        Spacer(Modifier.height(6.dp))

                                        Text(
                                            text = "📎 ${a.attachmentName}",
                                            color = PrimaryPurple,
                                            modifier = Modifier.clickable {
                                                uriHandler.openUri(a.attachmentUrl)
                                            }
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    Button(onClick = {
                                        navController.navigate(
                                            "submission/${a.assignmentId}/${a.courseId}"
                                        )
                                    }) {
                                        Text(if (isStudent) "View / Submit" else "View Submissions")
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }

            "Chat" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ChatHub(
                            courseId = courseId,
                            currentRole = currentRole,
                            currentUid = currentUid,
                            currentName = currentName,
                            enrolledStudents = students,
                            lecturerId = course?.lectureId ?: "",
                            lecturerName = course?.lecturerName ?: "",
                            navController = navController
                        )
                    }
                }
            }

            "Grades" -> {
                GradesTab(
                    submissionVm = submissionVm,
                    assignments = assignments
                )
            }

            "Students" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { SectionHeader("Enrolled Students", "Who is taking this course") }
                    if (students.isEmpty()) {
                        item { EmptyCard("No students enrolled") }
                    } else {
                        items(students) { student ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(14.dp)) {
                                    Icon(Icons.Outlined.Group, null, tint = PrimaryPurple)
                                    Spacer(Modifier.width(10.dp))
                                    Text(student)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }            }
        }
    }

    if (showAssignmentDialog) {
        CreateAssignmentDialog(
            courseId = courseId,
            vm = assignmentVm,
            onDismiss = { showAssignmentDialog = false },
            onCreated = { showAssignmentDialog = false }
        )
    }
}


// ── GRADES TAB ──
@Composable
fun GradesTab(
    submissionVm: SubmissionViewModel,
    assignments: List<com.example.digitaluniversity.dashboard.assignment.Assignment>
) {
    val mySubmissions by submissionVm.mySubmissionsForCourse.observeAsState(emptyList())

    // Map assignmentId -> submission for quick lookup
    val submissionMap = remember(mySubmissions) {
        mySubmissions.associateBy { it.assignmentId }
    }

    // Average grade (only graded ones)
    val gradedList = mySubmissions.filter { it.grade.isNotBlank() }
    val average = if (gradedList.isEmpty()) null
    else gradedList.mapNotNull { it.grade.toFloatOrNull() }.average()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Average banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(PrimaryPurple),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Course Average", color = Color.White.copy(0.8f), fontSize = 13.sp)
                        Text(
                            if (average != null) String.format("%.1f / 5.0", average)
                            else "No grades yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                        Text(
                            "${gradedList.size} of ${assignments.size} graded",
                            color = Color.White.copy(0.7f),
                            fontSize = 12.sp
                        )
                    }
                    // Simple grade circle
                    if (average != null) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(0.2f), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                String.format("%.1f", average),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }
                    }
                }
            }
        }

        item { SectionHeader("Assignment Grades", "Your results per assignment") }

        if (assignments.isEmpty()) {
            item { EmptyCard("No assignments in this course yet") }
        } else {
            items(assignments) { assignment ->
                val submission = submissionMap[assignment.assignmentId]
                GradeCard(assignment = assignment, submission = submission)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun GradeCard(
    assignment: com.example.digitaluniversity.dashboard.assignment.Assignment,
    submission: com.example.digitaluniversity.dashboard.assignment.AssignmentSubmission?
) {
    val gradeColor = when {
        submission == null -> Color.Gray
        submission.grade.isBlank() -> Color(0xFFFF9800)
        else -> {
            val g = submission.grade.toFloatOrNull() ?: 0f
            when {
                g >= 4 -> Color(0xFF4CAF50)
                g >= 3 -> Color(0xFF2196F3)
                else   -> Color(0xFFF44336)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(assignment.title, fontWeight = FontWeight.Bold)
                Text("Due: ${assignment.dueDate}", color = Color.Gray, fontSize = 12.sp)

                if (submission == null) {
                    Text("Not submitted", color = Color.Gray, fontSize = 12.sp)
                } else if (submission.grade.isBlank()) {
                    Text("Submitted — awaiting grade", color = Color(0xFFFF9800), fontSize = 12.sp)
                } else if (submission.comment.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "\"${submission.comment}\"",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Grade badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(gradeColor.copy(0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        submission == null -> "—"
                        submission.grade.isBlank() -> "⏳"
                        else -> submission.grade
                    },
                    color = gradeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (submission?.grade?.isNotBlank() == true) 22.sp else 18.sp
                )
            }
        }
    }
}

@Composable
fun EmptyCard(text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}