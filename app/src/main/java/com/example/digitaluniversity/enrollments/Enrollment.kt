package com.example.digitaluniversity.enrollments

data class Enrollment(
    val courseId: String = "",
    val studentEmail: String = "",
    val enrolledAt: com.google.firebase.Timestamp? = null
)