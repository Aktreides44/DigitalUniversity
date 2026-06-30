package com.example.digitaluniversity.dashboard.assignment

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class SubmissionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _submissions = MutableLiveData<List<AssignmentSubmission>>()
    val submissions: LiveData<List<AssignmentSubmission>> = _submissions

    private val _uploadProgress = MutableLiveData<Float?>(null)
    val uploadProgress: LiveData<Float?> = _uploadProgress

    private val _mySubmission = MutableLiveData<AssignmentSubmission?>(null)
    val mySubmission: LiveData<AssignmentSubmission?> = _mySubmission

    // All submissions for this student across a whole course (for grades tab)
    private val _mySubmissionsForCourse = MutableLiveData<List<AssignmentSubmission>>()
    val mySubmissionsForCourse: LiveData<List<AssignmentSubmission>> = _mySubmissionsForCourse

    fun loadSubmissions(assignmentId: String) {
        db.collection("submissions")
            .whereEqualTo("assignmentId", assignmentId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                _submissions.value = snapshot.documents.mapNotNull {
                    it.toObject(AssignmentSubmission::class.java)
                }
            }
    }

    fun loadMySubmission(assignmentId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("submissions")
            .whereEqualTo("assignmentId", assignmentId)
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                _mySubmission.value = snapshot.documents
                    .firstOrNull()
                    ?.toObject(AssignmentSubmission::class.java)
            }
    }

    // Load ALL of this student's submissions for a course (grades tab)
    fun loadMySubmissionsForCourse(courseId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("submissions")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                _mySubmissionsForCourse.value = snapshot.documents.mapNotNull {
                    it.toObject(AssignmentSubmission::class.java)
                }
            }
    }

    fun uploadAndSubmit(
        fileUri: Uri,
        fileName: String,
        assignmentId: String,
        courseId: String,
        studentName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            onResult(false, "Not logged in"); return
        }
        val submissionId = System.currentTimeMillis().toString()
        val storageRef = storage.reference
            .child("submissions/$assignmentId/$uid/$fileName")

        _uploadProgress.value = 0f

        storageRef.putFile(fileUri)
            .addOnProgressListener { task ->
                _uploadProgress.value =
                    (100.0 * task.bytesTransferred / task.totalByteCount).toFloat()
            }
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val submission = AssignmentSubmission(
                        submissionId = submissionId,
                        assignmentId = assignmentId,
                        courseId = courseId,
                        studentId = uid,
                        studentName = studentName,
                        fileUrl = uri.toString(),
                        fileName = fileName,
                        submittedAt = SimpleDateFormat(
                            "dd/MM/yyyy HH:mm", Locale.getDefault()
                        ).format(Date()),
                        grade = "",
                        comment = ""
                    )
                    db.collection("submissions").document(submissionId).set(submission)
                        .addOnSuccessListener { onResult(true, "Submitted!") }
                        .addOnFailureListener { onResult(false, it.message ?: "Save failed") }
                    _uploadProgress.value = null
                }
            }
            .addOnFailureListener { e ->
                _uploadProgress.value = null
                onResult(false, e.message ?: "Upload failed")
            }
    }

    fun gradeSubmission(
        submissionId: String,
        grade: String,
        comment: String,
        onResult: (Boolean, String) -> Unit
    ) {
        db.collection("submissions").document(submissionId)
            .update(mapOf("grade" to grade, "comment" to comment))
            .addOnSuccessListener { onResult(true, "Grade saved") }
            .addOnFailureListener { onResult(false, it.message ?: "Failed") }
    }
}