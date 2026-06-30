package com.example.digitaluniversity.enrollments


import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

class EnrollmentRepository {

    private val db = FirebaseFirestore.getInstance()

    fun enrollStudent(
        studentEmail: String,
        courseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val enrollment = hashMapOf(
            "studentEmail" to studentEmail,
            "courseId" to courseId,
            "enrolledAt" to Timestamp.now()
        )

        db.collection("enrollments")
            .add(enrollment)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Enrollment failed")
            }
    }
}