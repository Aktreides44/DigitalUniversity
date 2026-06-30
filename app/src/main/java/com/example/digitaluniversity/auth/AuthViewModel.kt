package com.example.digitaluniversity.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()

    val success = MutableLiveData<String>()
    val error = MutableLiveData<String>()

    fun login(email: String, password: String) {
        repo.login(
            email,
            password,
            onSuccess = { role ->
                success.postValue(role) // ONLY role
            },
            onError = { error.postValue(it) }
        )
    }
    val lecturers = MutableLiveData<List<User>>()

    fun loadLecturers() {
        repo.getLecturers {
            lecturers.postValue(it)
        }
    }


    fun register(name: String, email: String, password: String, role: String) {
        repo.register(
            name,
            email,
            password,
            role,
            onSuccess = { success.postValue("Registered successfully") },
            onError = { error.postValue(it) }
        )
    }

}