package com.example.digitaluniversity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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

fun getPasswordStrength(password: String): String {
    return when {
        password.length < 6 -> "Weak"
        password.length in 6..9 -> "Medium"
        password.length >= 10 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } -> "Strong"
        else -> "Medium"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    vm: AuthViewModel = viewModel()
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val roles = listOf("student", "lecturer")
    var role by remember { mutableStateOf(roles[0]) }

    val error by vm.error.observeAsState()
    val success by vm.success.observeAsState()
    val passwordStrength = getPasswordStrength(password)

    val strengthColor = when (passwordStrength) {
        "Weak" -> Color(0xFFC62828)
        "Medium" -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }

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
                modifier = Modifier.fillMaxWidth().padding(top = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("DU", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(Modifier.height(12.dp))
                Text("Create Account", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Join Digital University",
                    color = Color.White.copy(0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Sign Up", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Fill in your details below", color = Color.Gray, fontSize = 14.sp)

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Outlined.Person, null, tint = PrimaryPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple, focusedLabelColor = PrimaryPurple
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Outlined.Email, null, tint = PrimaryPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple, focusedLabelColor = PrimaryPurple
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
                                    if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    null, tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple, focusedLabelColor = PrimaryPurple
                        )
                    )

                    // Strength bar
                    if (password.isNotBlank()) {
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Weak", "Medium", "Strong").forEachIndexed { index, _ ->
                                    val active = when (passwordStrength) {
                                        "Weak" -> index == 0
                                        "Medium" -> index <= 1
                                        else -> true
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(if (active) strengthColor else Color(0xFFE0E0E0))
                                    )
                                }
                            }
                            Text(
                                "Strength: $passwordStrength",
                                color = strengthColor,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = PrimaryPurple) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    null, tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple, focusedLabelColor = PrimaryPurple
                        ),
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotBlank() && password != confirmPassword)
                                Text("Passwords do not match", color = Color(0xFFC62828))
                        }
                    )

                    // Role chips
                    Text("I am a:", color = Color.Gray, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        roles.forEach { r ->
                            val selected = role == r
                            FilterChip(
                                selected = selected,
                                onClick = { role = r },
                                label = {
                                    Text(
                                        r.replaceFirstChar { it.uppercase() },
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    error?.let {
                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFFEBEE)) {
                            Text(it, color = Color(0xFFC62828), modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            when {
                                name.isBlank() || email.isBlank() ||
                                        password.isBlank() || confirmPassword.isBlank() ->
                                    vm.error.postValue("Please fill in all fields")
                                passwordStrength == "Weak" ->
                                    vm.error.postValue("Password is too weak")
                                password != confirmPassword ->
                                    vm.error.postValue("Passwords do not match")
                                else -> vm.register(name, email, password, role)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Already have an account? ", color = Color.Gray)
                        Text("Sign In", color = PrimaryPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    success?.let {
        LaunchedEffect(it) { navController.popBackStack() }
    }
}