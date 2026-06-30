package com.example.digitaluniversity.dashboard.assignment

data class Assignment(
    val assignmentId: String = "",
    val courseId: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",
    val createdBy: String = "",
    val attachmentUrl: String = "",   // ← new: file URL
    val attachmentName: String = ""   // ← new: display name
)