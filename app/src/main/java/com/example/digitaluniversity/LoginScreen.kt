package com.example.digitaluniversity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.auth.AuthViewModel
import com.example.digitaluniversity.dashboard.PrimaryPurple
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun LoginScreen(
    navController: NavController,
    vm: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val error by vm.error.observeAsState()
    val success by vm.success.observeAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        PrimaryPurple.copy(alpha = 0.9f),
                        PrimaryPurple,
                        PrimaryPurple.copy(alpha = 0.7f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Branding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DU", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(16.dp))
                Text("Digital University", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Your learning journey starts here",
                    color = Color.White.copy(0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Welcome back", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Sign in to continue", color = Color.Gray, fontSize = 14.sp)

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = PrimaryPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            focusedLabelColor = PrimaryPurple
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = PrimaryPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.Visibility
                                    else Icons.Outlined.VisibilityOff,
                                    null, tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            focusedLabelColor = PrimaryPurple
                        )
                    )

                    error?.let {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFEBEE)) {
                            Text(it, color = Color(0xFFC62828), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = { vm.login(email, password) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = { navController.navigate("register") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Don't have an account? ", color = Color.Gray)
                        Text("Register", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    success?.let { role ->
        LaunchedEffect(role) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .update("fcmToken", token)
                }
            }
            when (role) {
                "student" -> navController.navigate("student")
                "lecturer" -> navController.navigate("lecturer")
                "admin" -> navController.navigate("admin")
            }
        }
    }
}