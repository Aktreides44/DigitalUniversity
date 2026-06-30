package com.example.digitaluniversity.courses

import com.example.digitaluniversity.courses.Course
import com.google.firebase.firestore.FirebaseFirestore

class CourseRepository {

    private val db = FirebaseFirestore.getInstance()

    // GET ALL COURSES
    fun getCourses(
        onResult: (List<Course>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("courses")
            .get()
            .addOnSuccessListener { snapshot ->
                val courses = snapshot.documents.mapNotNull {
                    it.toObject(Course::class.java)
                }
                onResult(courses)
            }
            .addOnFailureListener {
                onError(it.message ?: "Error loading courses")
            }
    }

    // CREATE COURSE (ADMIN)
    fun createCourse(course: Course, onSuccess: () -> Unit) {
        db.collection("courses")
            .document(course.courseId)
            .set(course)
            .addOnSuccessListener { onSuccess() }
    }

    fun enrollStudent(
        studentEmail: String,
        courseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val enrollment = hashMapOf(
            "studentEmail" to studentEmail,
            "courseId" to courseId,
            "enrolledAt" to com.google.firebase.Timestamp.now()
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
    fun createCourseMap(course: Map<String, Any>, onSuccess: () -> Unit) {
        db.collection("courses")
            .document(course["courseId"].toString())
            .set(course)
            .addOnSuccessListener { onSuccess() }
    }
    fun getEnrolledCourses(
        studentEmail: String,
        onResult: (List<String>) -> Unit
    ) {
        db.collection("enrollments")
            .whereEqualTo("studentEmail", studentEmail)
            .get()
            .addOnSuccessListener { snap ->
                val list = snap.documents.mapNotNull {
                    it.getString("courseId")
                }
                onResult(list)
            }
    }
    fun getLecturerCourses(
        lecturerId: String,
        onResult: (List<Course>) -> Unit,
        onError: (String) -> Unit
    ) {

        db.collection("courses")
            .whereEqualTo("lectureId", lecturerId)
            .get()
            .addOnSuccessListener { snapshot ->

                val courses = snapshot.documents.mapNotNull {
                    it.toObject(Course::class.java)
                }

                onResult(courses)
            }
            .addOnFailureListener {
                onError(it.message ?: "Failed to load lecturer courses")
            }
    }

    fun getStudentsInCourse(
        courseId: String,
        onResult: (List<String>) -> Unit
    ) {

        db.collection("enrollments")
            .whereEqualTo("courseId", courseId)
            .get()
            .addOnSuccessListener { snap ->

                val students = snap.documents.mapNotNull {
                    it.getString("studentEmail")
                }

                onResult(students)
            }
    }

}