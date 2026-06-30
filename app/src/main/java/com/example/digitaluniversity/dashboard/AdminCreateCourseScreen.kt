package com.example.digitaluniversity.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.auth.AuthViewModel
import com.example.digitaluniversity.courses.Course
import com.example.digitaluniversity.courses.CourseSchedule
import com.example.digitaluniversity.courses.CourseViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreateCourseScreen(
    navController: NavController,
    vm: CourseViewModel = viewModel(),
    authVm: AuthViewModel = viewModel()
) {
    // ── Course-level fields ──
    var courseName     by remember { mutableStateOf("") }
    var courseCode     by remember { mutableStateOf("") }
    var courseId       by remember { mutableStateOf("") }
    var credits        by remember { mutableStateOf("") }
    var description    by remember { mutableStateOf("") }

    // ── Lecturer dropdown ──
    val lecturers by authVm.lecturers.observeAsState(emptyList())
    var expanded              by remember { mutableStateOf(false) }
    var selectedLecturerName  by remember { mutableStateOf("") }
    var selectedLecturerId    by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { authVm.loadLecturers() }

    // ── Session state ──
    val sessions = remember { mutableStateListOf<CourseSchedule>() }

    // ── "Add session" dialog state ──
    var showSessionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
            .verticalScroll(rememberScrollState())
    ) {

        Card(
            shape = RoundedCornerShape(
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = PrimaryPurple
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Create Course",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Create and assign university courses",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

        // ── Course details ──
        TextField(value = courseName,   onValueChange = { courseName = it },   label = { Text("Course Name") },   modifier = Modifier.fillMaxWidth())
        TextField(value = courseCode,   onValueChange = { courseCode = it },   label = { Text("Course Code") },   modifier = Modifier.fillMaxWidth())
        TextField(value = courseId,     onValueChange = { courseId = it },     label = { Text("Course ID") },     modifier = Modifier.fillMaxWidth())
        TextField(value = credits,      onValueChange = { credits = it },      label = { Text("Credits") },       modifier = Modifier.fillMaxWidth())
        TextField(value = description,  onValueChange = { description = it },  label = { Text("Description") },   modifier = Modifier.fillMaxWidth())

        // ── Lecturer dropdown ──
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                value = selectedLecturerName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Assign Lecturer") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                lecturers.forEach { lecturer ->
                    DropdownMenuItem(
                        text = { Text(lecturer.name) },
                        onClick = {
                            selectedLecturerName = lecturer.name
                            selectedLecturerId   = lecturer.uid
                            expanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Sessions section ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(onClick = { showSessionDialog = true }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Add Session")
            }
        }

        if (sessions.isEmpty()) {
            Text("No sessions added yet", color = Color.Gray)
        } else {
            sessions.forEachIndexed { index, session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryPurple.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(session.day, fontWeight = FontWeight.Bold)
                            Text("${session.startTime} – ${session.endTime}", color = PrimaryPurple)
                            Text("Room: ${session.room}", color = Color.Gray)
                        }
                        IconButton(onClick = { sessions.removeAt(index) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = Color.Red)
                        }
                    }
                }
            }
        }

        // ── Create button ──
        Button(
            onClick = {
                val course = Course(
                    courseId     = courseId,
                    courseCode   = courseCode,
                    courseName   = courseName,
                    credits      = credits.toIntOrNull() ?: 0,
                    description  = description,
                    lecturerName = selectedLecturerName,
                    lectureId    = selectedLecturerId,
                    schedules    = sessions.toList()
                )
                vm.createCourse(course) { navController.popBackStack() }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = courseId.isNotBlank() && courseName.isNotBlank() && sessions.isNotEmpty()
        ) {
            Text("Create Course")
        }
    }

    // ══════════════════════════════════════
    //  Add Session Dialog
    // ══════════════════════════════════════
    if (showSessionDialog) {
        AddSessionDialog(
            onDismiss = { showSessionDialog = false },
            onConfirm = { newSession ->
                sessions.add(newSession)
                showSessionDialog = false
            }
        )
    }
}}

// ──────────────────────────────────────────────────────────────
//  Add Session Dialog
// ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (CourseSchedule) -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

    var day            by remember { mutableStateOf("") }
    var room           by remember { mutableStateOf("") }
    var startTime      by remember { mutableStateOf("") }
    var endTime        by remember { mutableStateOf("") }
    var dayExpanded    by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker   by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Day picker
                ExposedDropdownMenuBox(expanded = dayExpanded, onExpandedChange = { dayExpanded = !dayExpanded }) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = dayExpanded, onDismissRequest = { dayExpanded = false }) {
                        days.forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { day = it; dayExpanded = false })
                        }
                    }
                }

                // Start time
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Start Time") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showStartPicker = true }) { Text("Pick") }
                }

                // End time
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("End Time") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { showEndPicker = true }) { Text("Pick") }
                }

                // Room
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(CourseSchedule(day = day, startTime = startTime, endTime = endTime, room = room)) },
                enabled = day.isNotBlank() && startTime.isNotBlank() && endTime.isNotBlank() && room.isNotBlank()
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    // ── Time pickers (outside the AlertDialog to avoid nesting issues) ──
    if (showStartPicker) {
        TimePickerDialog(
            title = "Select Start Time",
            onDismiss = { showStartPicker = false },
            onConfirm = { h, m -> startTime = String.format("%02d:%02d", h, m); showStartPicker = false }
        )
    }
    if (showEndPicker) {
        TimePickerDialog(
            title = "Select End Time",
            onDismiss = { showEndPicker = false },
            onConfirm = { h, m -> endTime = String.format("%02d:%02d", h, m); showEndPicker = false }
        )
    }
}

// ──────────────────────────────────────────────────────────────
//  Reusable Time Picker Dialog
// ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val now   = Calendar.getInstance()
    val state = rememberTimePickerState(
        initialHour   = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour      = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { TimePicker(state = state) },
        confirmButton = {
            Button(onClick = { onConfirm(state.hour, state.minute) }) { Text("Confirm") }
        }
    )
}