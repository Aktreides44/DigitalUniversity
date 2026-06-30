package com.example.digitaluniversity.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = ""
)

@Composable
fun UsersScreen(navController: NavController) {
    var users by remember { mutableStateOf(listOf<AppUser>()) }
    var userToDelete by remember { mutableStateOf<AppUser?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    fun loadUsers() {
        FirebaseFirestore.getInstance().collection("users").get()
            .addOnSuccessListener { result ->
                users = result.documents.map {
                    AppUser(
                        uid = it.id,
                        name = it.getString("name") ?: "",
                        email = it.getString("email") ?: "",
                        role = it.getString("role") ?: ""
                    )
                }
                isLoading = false
            }
    }

    LaunchedEffect(Unit) { loadUsers() }

    Scaffold(
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = { TextButton(onClick = { snackbarMessage = null }) { Text("OK") } }
                ) { Text(msg) }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("System Users", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Students, lecturers and administrators", color = Color.Gray)
                Spacer(Modifier.height(10.dp))
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                }
            }

            val grouped = users.groupBy { it.role.lowercase() }
            listOf("admin", "lecturer", "student").forEach { roleKey ->
                val group = grouped[roleKey] ?: return@forEach
                item {
                    Text(
                        "${roleKey.replaceFirstChar { it.uppercase() }}s (${group.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryPurple,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(group) { user ->
                    UserCard(user = user, onDeleteClick = { userToDelete = user })
                }
            }

            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    // ── CASCADE DELETE USER DIALOG ──
    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { if (!isDeleting) userToDelete = null },
            title = { Text("Delete User") },
            text = {
                Text(
                    "Delete ${user.name}?\n\n" +
                            "This will also remove all their enrollments and submissions."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        cascadeDeleteUser(
                            uid = user.uid,
                            email = user.email,
                            onDone = {
                                users = users.filter { it.uid != user.uid }
                                userToDelete = null
                                isDeleting = false
                                snackbarMessage = "${user.name} deleted"
                            },
                            onError = { err ->
                                isDeleting = false
                                snackbarMessage = "Error: $err"
                            }
                        )
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isDeleting) userToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ── CASCADE DELETE USER: Firestore doc + enrollments + submissions ──
// Note: Firebase Auth user deletion requires Admin SDK (Cloud Function)
// This deletes the Firestore data; the Auth account becomes orphaned but can't log in
fun cascadeDeleteUser(
    uid: String,
    email: String,
    onDone: () -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    // Delete user doc
    batch.delete(db.collection("users").document(uid))

    var pending = 2
    var done = 0

    fun tryCommit() {
        done++
        if (done >= pending) {
            batch.commit()
                .addOnSuccessListener { onDone() }
                .addOnFailureListener { onError(it.message ?: "Failed") }
        }
    }

    // Delete enrollments by email
    db.collection("enrollments").whereEqualTo("studentEmail", email).get()
        .addOnSuccessListener { snap ->
            snap.documents.forEach { batch.delete(it.reference) }
            tryCommit()
        }
        .addOnFailureListener { tryCommit() } // proceed even if this fails

    // Delete submissions by studentId
    db.collection("submissions").whereEqualTo("studentId", uid).get()
        .addOnSuccessListener { snap ->
            snap.documents.forEach { batch.delete(it.reference) }
            tryCommit()
        }
        .addOnFailureListener { tryCommit() }
}

@Composable
fun UserCard(user: AppUser, onDeleteClick: () -> Unit) {
    val roleColor = when (user.role.lowercase()) {
        "admin" -> Color(0xFFE91E63)
        "lecturer" -> Color(0xFF2196F3)
        else -> PrimaryPurple
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = roleColor.copy(0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        user.name.take(1).uppercase(),
                        color = roleColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(user.email, color = Color.Gray, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = roleColor) {
                    Text(
                        user.role.uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Outlined.Delete, null, tint = Color.Red.copy(0.7f))
            }
        }
    }
}