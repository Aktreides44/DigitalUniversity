package com.example.digitaluniversity.auth

import android.util.Log
import com.example.digitaluniversity.auth.User
import com.example.digitaluniversity.courses.Course
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

     private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun register(
        name: String,
        email: String,
        password: String,
        role: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = auth.currentUser?.uid ?: return@addOnSuccessListener

                val user = User(uid, name, email, role)

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it.message ?: "Firestore error") }
            }
            .addOnFailureListener {
                val message = it.message ?: "Unknown error"
                Log.e("FirebaseRegister", message, it)
                onError(message)
            }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {

                val uid = auth.currentUser?.uid

                if (uid == null) {
                    onError("User ID is null")
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->

                        if (!document.exists()) {
                            onError("User document does not exist")
                            return@addOnSuccessListener
                        }

                        val role = document.getString("role")

                        if (role.isNullOrEmpty()) {
                            onError("Role field missing")
                        } else {
                            onSuccess(role)
                        }
                    }
                    .addOnFailureListener {
                        onError(it.message ?: "Failed to fetch role")
                    }
            }
            .addOnFailureListener {
                onError(it.message ?: "Login failed")
            }
    }

    fun getLecturers(
        onResult: (List<User>) -> Unit
    ) {

        db.collection("users")
            .whereEqualTo("role", "lecturer")
            .get()
            .addOnSuccessListener { snapshot ->

                val lecturers = snapshot.documents.mapNotNull {
                    it.toObject(User::class.java)
                }

                onResult(lecturers)
            }
    }

}