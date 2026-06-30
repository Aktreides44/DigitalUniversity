package com.example.digitaluniversity.dashboard.assignment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.dashboard.BgGray
import com.example.digitaluniversity.dashboard.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionScreen(
    assignmentId: String,
    courseId: String,
    role: String,
    studentName: String,
    navController: NavController,
    vm: SubmissionViewModel = viewModel()
) {
    val context = LocalContext.current
    val submissions by vm.submissions.observeAsState(emptyList())
    val mySubmission by vm.mySubmission.observeAsState(null)
    val uploadProgress by vm.uploadProgress.observeAsState(null)

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) {
                    selectedFileName = it.getString(nameIndex)
                }
            }
            if (selectedFileName.isBlank()) selectedFileName = "file_${System.currentTimeMillis()}"
        }
    }

    val isStudent = role == "student"
    val isLecturer = role == "lecturer" || role == "admin"

    LaunchedEffect(assignmentId) {
        if (isStudent) vm.loadMySubmission(assignmentId)
        if (isLecturer) vm.loadSubmissions(assignmentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submissions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = Color.White)
                        }
                    }
                ) { Text(msg) }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ─── STUDENT VIEW ───
            if (isStudent) {
                item {
                    Text(
                        "Your Submission",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                item {
                    val existing = mySubmission
                    if (existing != null) {
                        // Already submitted — show status + grade + comment
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(Color.White)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Submitted",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("File: ${existing.fileName.ifBlank { "Uploaded file" }}")
                                Text(
                                    "Submitted at: ${existing.submittedAt}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Divider(color = Color(0xFFEEEEEE))
                                Spacer(Modifier.height(12.dp))

                                // Grade badge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Grade: ",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (existing.grade.isBlank()) {
                                        Text("Pending", color = Color.Gray)
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = PrimaryPurple
                                        ) {
                                            Text(
                                                existing.grade,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp, vertical = 4.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                // Lecturer comment
                                if (existing.comment.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF3EFFF)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Text(
                                                "Lecturer's comment",
                                                fontSize = 12.sp,
                                                color = PrimaryPurple,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(existing.comment, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Not yet submitted — file picker
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(Color.White)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            2.dp,
                                            PrimaryPurple.copy(alpha = 0.4f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { filePicker.launch("*/*") }
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.AttachFile,
                                            contentDescription = null,
                                            tint = PrimaryPurple,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        if (selectedFileName.isBlank()) {
                                            Text("Tap to choose a file", color = Color.Gray)
                                            Text(
                                                "PDF, DOC, images, etc.",
                                                color = Color.LightGray,
                                                fontSize = 12.sp
                                            )
                                        } else {
                                            Text(
                                                selectedFileName,
                                                fontWeight = FontWeight.Medium,
                                                color = PrimaryPurple
                                            )
                                            Text("Tap to change", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                uploadProgress?.let { progress ->
                                    Column {
                                        LinearProgressIndicator(
                                            progress = { progress / 100f },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = PrimaryPurple
                                        )
                                        Text(
                                            "Uploading... ${progress.toInt()}%",
                                            fontSize = 12.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }

                                Button(
                                    onClick = {
                                        val uri = selectedFileUri
                                        if (uri == null) {
                                            snackbarMessage = "Please choose a file first"
                                            return@Button
                                        }
                                        vm.uploadAndSubmit(
                                            fileUri = uri,
                                            fileName = selectedFileName,
                                            assignmentId = assignmentId,
                                            courseId = courseId,
                                            studentName = studentName
                                        ) { success, message ->
                                            snackbarMessage = message
                                            if (success) {
                                                selectedFileUri = null
                                                selectedFileName = ""
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = selectedFileUri != null && uploadProgress == null,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Icon(
                                        Icons.Outlined.Upload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Submit Assignment")
                                }
                            }
                        }
                    }
                }
            }

            // ─── LECTURER VIEW ───
            if (isLecturer) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text("Submissions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = PrimaryPurple
                        ) {
                            Text(
                                "${submissions.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (submissions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(Color.White)
                        ) {
                            Box(
                                Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No submissions yet", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(submissions) { submission ->
                        LecturerSubmissionCard(
                            submission = submission,
                            vm = vm,
                            onGraded = { snackbarMessage = it }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun LecturerSubmissionCard(
    submission: AssignmentSubmission,
    vm: SubmissionViewModel,
    onGraded: (String) -> Unit
) {
    // Pre-fill with existing grade/comment if already graded
    var selectedGrade by remember(submission.submissionId) {
        mutableStateOf(
            if (submission.grade.isNotBlank()) submission.grade.toIntOrNull() ?: 0 else 0
        )
    }
    var comment by remember(submission.submissionId) { mutableStateOf(submission.comment) }
    var saving by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            // Student name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(submission.studentName, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(6.dp))

            // Tappable file download link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        if (submission.fileUrl.isNotBlank()) {
                            uriHandler.openUri(submission.fileUrl)
                        }
                    }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.AttachFile,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = submission.fileName.ifBlank { "Download file" },
                    color = PrimaryPurple,
                    fontSize = 13.sp,
                    textDecoration = TextDecoration.Underline
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = "Download",
                    tint = PrimaryPurple,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                "Submitted: ${submission.submittedAt}",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(12.dp))

            // ── GRADE PICKER (2–5) ──
            Text(
                "Grade",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                (2..5).forEach { grade ->
                    val isSelected = selectedGrade == grade
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) PrimaryPurple else Color(0xFFF0F0F0),
                        modifier = Modifier
                            .size(52.dp)
                            .clickable { selectedGrade = grade }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "$grade",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (isSelected) Color.White else Color.DarkGray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── COMMENT FIELD ──
            Text(
                "Comment",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text("Optional feedback for the student...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(Modifier.height(12.dp))

            // ── SAVE BUTTON ──
            Button(
                onClick = {
                    saving = true
                    vm.gradeSubmission(
                        submissionId = submission.submissionId,
                        grade = selectedGrade.toString(),
                        comment = comment
                    ) { _, msg ->
                        saving = false
                        onGraded(msg)
                    }
                },
                enabled = !saving && selectedGrade in 2..5,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Grade & Comment")
                }
            }
        }
    }
}