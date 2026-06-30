package com.example.digitaluniversity.dashboard.assignment

data class AssignmentSubmission(
    val submissionId: String = "",
    val assignmentId: String = "",
    val courseId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val submittedAt: String = "",
    val grade: String = "",
    val comment: String = ""   // ← new
)