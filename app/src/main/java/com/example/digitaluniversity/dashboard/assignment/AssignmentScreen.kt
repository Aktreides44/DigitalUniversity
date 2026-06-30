package com.example.digitaluniversity.dashboard.assignment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.dashboard.PrimaryPurple
import com.example.digitaluniversity.dashboard.assignment.Assignment
import com.example.digitaluniversity.dashboard.assignment.AssignmentViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AssignmentScreen(
    courseId: String,
    vm: AssignmentViewModel = viewModel(),
    navController: NavController,
    role: String
) {
    val assignments by vm.assignments.observeAsState(emptyList())
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(courseId) { vm.loadAssignments(courseId) }

    val canCreate = role.trim().lowercase().let { it == "lecturer" || it == "admin" }

    Scaffold(
        floatingActionButton = {
            if (canCreate) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = PrimaryPurple
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Assignments", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Course coursework and submissions", color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
            }

            if (assignments.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) { Text("No assignments created yet") }
                    }
                }
            }

            items(assignments) { assignment ->
                AssignmentCard(
                    assignment = assignment,
                    role = role,
                    navController = navController
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDialog) {
        CreateAssignmentDialog(
            courseId = courseId,
            vm = vm,
            onDismiss = { showDialog = false },
            onCreated = { showDialog = false }
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    role: String,
    navController: NavController
) {

    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(assignment.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(assignment.description)
            Spacer(Modifier.height(6.dp))
            Text("📅 Due: ${assignment.dueDate}", color = PrimaryPurple, fontWeight = FontWeight.SemiBold)


            // Attachment download link
            if (assignment.attachmentUrl.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { uriHandler.openUri(assignment.attachmentUrl) }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Outlined.AttachFile, null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        assignment.attachmentName.ifBlank { "Download attachment" },
                        color = PrimaryPurple,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Outlined.Download, null, tint = PrimaryPurple, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    navController.navigate("submission/${assignment.assignmentId}/${assignment.courseId}")
                }
            ) {
                Text(if (role == "student") "View / Submit" else "View Submissions")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAssignmentDialog(
    courseId: String,
    vm: AssignmentViewModel,
    onDismiss: () -> Unit,
    onCreated: () -> Unit
) {
    val context = LocalContext.current
    val uploadProgress by vm.uploadProgress.observeAsState(null)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var isCreating by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (it.moveToFirst() && nameIndex >= 0) selectedFileName = it.getString(nameIndex)
            }
            if (selectedFileName.isBlank()) selectedFileName = "file_${System.currentTimeMillis()}"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Assignment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Due Date") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Select Due Date")
                }

                // File attachment section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, PrimaryPurple.copy(0.4f), RoundedCornerShape(10.dp))
                        .clickable { filePicker.launch("*/*") }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AttachFile, null, tint = PrimaryPurple)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selectedFileName.isBlank()) "Attach a file (optional)"
                            else selectedFileName,
                            color = if (selectedFileName.isBlank()) Color.Gray else PrimaryPurple
                        )
                    }
                }

                // Upload progress
                uploadProgress?.let { progress ->
                    Column {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryPurple
                        )
                        Text("Uploading... ${progress.toInt()}%", color = Color.Gray,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isCreating = true
                    val assignment = Assignment(
                        assignmentId = System.currentTimeMillis().toString(),
                        courseId = courseId,
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        createdBy = com.google.firebase.auth.FirebaseAuth
                            .getInstance().currentUser?.uid ?: ""
                    )
                    android.util.Log.d(
                        "ASSIGNMENT_DEBUG",
                        "URI=$selectedFileUri FILE=$selectedFileName"
                    )
                    vm.createAssignmentWithFile(
                        assignment = assignment,
                        fileUri = selectedFileUri,
                        fileName = selectedFileName
                    ) { success, _ ->
                        isCreating = false
                        if (success) onCreated()
                    }
                },
                // null = not uploading = button enabled
                enabled = title.isNotBlank() && description.isNotBlank()
                        && dueDate.isNotBlank() && !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        dueDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}