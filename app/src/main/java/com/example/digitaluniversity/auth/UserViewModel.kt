package com.example.digitaluniversity.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserViewModel : ViewModel() {

    private val _role = MutableLiveData<String>("")
    val role: LiveData<String> = _role

    private val _displayName = MutableLiveData<String>("")
    val displayName: LiveData<String> = _displayName

    fun loadCurrentUser() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                _role.value = doc.getString("role")?.trim()?.lowercase() ?: ""
                _displayName.value = doc.getString("name")
                    ?: user.displayName
                            ?: "User"

                // Ensure email is stored in Firestore so lecturer DM lookup works
                if (doc.getString("email").isNullOrBlank() && user.email != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("email", user.email!!)
                }
            }
    }
}