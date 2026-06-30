package com.example.digitaluniversity.courses

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore


class CourseViewModel : ViewModel() {

    private val repo = CourseRepository()

    val courses = MutableLiveData<List<Course>>()
    val enrolledStudents = MutableLiveData<List<String>>()
    val selectedCourse = MutableLiveData<Course>()
    val error = MutableLiveData<String>()

    // 🔄 Load all courses (for student + lecturer)
    fun loadCourses() {
        repo.getCourses(
            onResult = {
                courses.postValue(it)
            },
            onError = {
                error.postValue(it)
            }
        )
    }

    // 🛠 Admin creates course
    fun createCourse(course: Course, onDone: () -> Unit) {
        repo.createCourse(course) {
            onDone()
            loadCourses() // refresh list after creation
        }
    }
    fun loadStudentsInCourse(courseId: String) {

        repo.getStudentsInCourse(courseId) {
            enrolledStudents.postValue(it)
        }
    }
    fun enrollStudent(
        studentEmail: String,
        courseId: String,
        onDone: () -> Unit = {}
    ) {
        repo.enrollStudent(
            studentEmail = studentEmail,
            courseId = courseId,
            onSuccess = {
                onDone()
            },
            onError = {
                error.postValue(it)
            }
        )
    }
    fun createCourseFromMap(course: Map<String, Any>) {
        repo.createCourseMap(course) {
            loadCourses()
        }
    }
    fun loadLecturerCourses(lecturerId: String) {

        repo.getLecturerCourses(
            lecturerId = lecturerId,
            onResult = {
                courses.postValue(it)
            },
            onError = {
                error.postValue(it)
            }
        )
    }
    fun getEnrolledCourses(studentEmail: String, onResult: (List<String>) -> Unit) {
        repo.getEnrolledCourses(studentEmail, onResult)
    }

    fun isEnrolled(studentEmail: String, courseId: String, onResult: (Boolean) -> Unit) {
        repo.getEnrolledCourses(studentEmail) { list ->
            onResult(list.contains(courseId))
        }
    }
    fun updateCourse(
        course: Course,
        onComplete: () -> Unit
    ) {
        FirebaseFirestore.getInstance()
            .collection("courses")
            .document(course.courseId)
            .set(course)
            .addOnSuccessListener {
                onComplete()
            }
    }
    fun loadCourse(courseId: String) {

        repo.getCourses(
            onResult = { list ->

                val course = list.find {
                    it.courseId == courseId
                }

                if (course != null) {
                    selectedCourse.postValue(course)
                }
            },
            onError = {
                error.postValue(it)
            }
        )
    }
}