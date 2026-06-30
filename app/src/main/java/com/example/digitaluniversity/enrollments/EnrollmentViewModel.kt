package com.example.digitaluniversity.enrollments



import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EnrollmentViewModel : ViewModel() {

    private val repo = EnrollmentRepository()

    val success = MutableLiveData<String>()
    val error = MutableLiveData<String>()

    // 🎓 Student enrolls in course
    fun enroll(studentEmail: String, courseId: String) {
        repo.enrollStudent(
            studentEmail,
            courseId,
            onSuccess = {
                success.postValue("Enrolled successfully")
            },
            onError = {
                error.postValue(it)
            }
        )
    }
}