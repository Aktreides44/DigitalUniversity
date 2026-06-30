package com.example.digitaluniversity.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileScreen(
    navController: NavController
) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(Unit) {

        currentUser?.uid?.let { uid ->

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener {

                    name = it.getString("name") ?: ""
                    email = it.getString("email") ?: ""
                    role = it.getString("role") ?: ""
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Icon(
            Icons.Outlined.Person,
            contentDescription = null,
            tint = PrimaryPurple,
            modifier = Modifier.size(90.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = name,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = email,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = PrimaryPurple
            ),
            shape = RoundedCornerShape(14.dp)
        ) {

            Text(
                text = role.uppercase(),
                color = Color.White,
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    "User Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Name: $name")
                Spacer(modifier = Modifier.height(8.dp))

                Text("Email: $email")
                Spacer(modifier = Modifier.height(8.dp))

                Text("Role: $role")
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {

                FirebaseAuth.getInstance().signOut()

                navController.navigate("login") {
                    popUpTo(0)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {

            Text("Logout")
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}