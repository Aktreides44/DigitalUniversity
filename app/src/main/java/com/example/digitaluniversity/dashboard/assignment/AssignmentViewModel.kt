package com.example.digitaluniversity.dashboard.assignment

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AssignmentViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _assignments = MutableLiveData<List<Assignment>>()
    val assignments: LiveData<List<Assignment>> = _assignments

    private val _uploadProgress = MutableLiveData<Float?>(null)
    val uploadProgress: LiveData<Float?> = _uploadProgress

    fun loadAssignments(courseId: String) {
        db.collection("assignments")
            .whereEqualTo("courseId", courseId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                _assignments.value = snapshot.documents.mapNotNull {
                    it.toObject(Assignment::class.java)
                }
            }
    }

    // Create without file
    fun createAssignment(assignment: Assignment, onResult: (Boolean, String) -> Unit) {
        db.collection("assignments")
            .document(assignment.assignmentId)
            .set(assignment)
            .addOnSuccessListener { onResult(true, "Assignment created") }
            .addOnFailureListener { onResult(false, it.message ?: "Error") }
    }

    // Create with optional file attachment
    fun createAssignmentWithFile(
        assignment: Assignment,
        fileUri: Uri?,
        fileName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        android.util.Log.d(
            "ASSIGNMENT_DEBUG",
            "createAssignmentWithFile called"
        )
        if (fileUri == null) {
            createAssignment(assignment, onResult)
            return
        }

        val storageRef = storage.reference
            .child("assignment_files/${assignment.assignmentId}/$fileName")

        _uploadProgress.value = 0f

        storageRef.putFile(fileUri)
            .addOnProgressListener { task ->
                _uploadProgress.value =
                    (100.0 * task.bytesTransferred / task.totalByteCount).toFloat()
            }
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    _uploadProgress.value = null
                    val withFile = assignment.copy(
                        attachmentUrl = uri.toString(),
                        attachmentName = fileName
                    )
                    createAssignment(withFile, onResult)
                }
            }
            .addOnFailureListener { e ->
                _uploadProgress.value = null
                onResult(false, e.message ?: "Upload failed")
            }
    }
}