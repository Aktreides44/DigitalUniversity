package com.example.digitaluniversity.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.digitaluniversity.dashboard.BgGray
import com.example.digitaluniversity.dashboard.PrimaryPurple
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@Composable
fun ChatHub(
    courseId: String,
    currentRole: String,
    currentUid: String,
    currentName: String,
    enrolledStudents: List<String>,
    lecturerId: String,
    lecturerName: String,
    navController: NavController,
    vm: ChatViewModel = viewModel()
) {
    val groupChatId = "group_$courseId"
    val isStudent = currentRole == "student"
    val isLecturer = currentRole == "lecturer" || currentRole == "admin"

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── GROUP CHAT ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    navController.navigate(
                        "chat_room/$groupChatId/Group Chat/$currentRole/$currentName"
                    )
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(PrimaryPurple)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Groups, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Group Chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Everyone in this course", color = Color.White.copy(0.8f), fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Outlined.ChevronRight, null, tint = Color.White)
            }
        }

        Text(
            "Direct Messages",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.Gray
        )

        if (isStudent) {
            // Student → one DM with lecturer, use own UID
            DmCard(
                name = lecturerName.ifBlank { "Lecturer" },
                subtitle = "Course lecturer",
                onClick = {
                    vm.openOrCreateDm(
                        courseId = courseId,
                        studentEmail = "",
                        studentIdDirect = currentUid,  // student passes their own UID
                        studentName = currentName,
                        lecturerId = lecturerId,
                        lecturerName = lecturerName
                    ) { chatId, otherName ->
                        navController.navigate(
                            "chat_room/$chatId/$otherName/$currentRole/$currentName"
                        )
                    }
                }
            )
        }

        if (isLecturer) {
            // Lecturer → one DM per enrolled student, resolve UID from email
            if (enrolledStudents.isEmpty()) {
                Text("No students enrolled yet", color = Color.Gray, fontSize = 13.sp)
            } else {
                enrolledStudents.forEach { studentEmail ->
                    DmCard(
                        name = studentEmail,
                        subtitle = "Student",
                        onClick = {
                            vm.openOrCreateDm(
                                courseId = courseId,
                                studentEmail = studentEmail,  // lecturer passes email to resolve UID
                                studentIdDirect = "",
                                studentName = studentEmail,
                                lecturerId = currentUid,
                                lecturerName = currentName
                            ) { chatId, otherName ->
                                navController.navigate(
                                    "chat_room/$chatId/$otherName/$currentRole/$currentName"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DmCard(name: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.take(1).uppercase(), color = PrimaryPurple, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Outlined.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatId: String,
    chatTitle: String,
    currentRole: String,
    currentName: String,
    navController: NavController,
    vm: ChatViewModel = viewModel()
) {
    val messages by vm.messages.observeAsState(emptyList())
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(chatId) { vm.loadMessages(chatId) }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chatTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (chatId.startsWith("group")) "Group Chat" else "Direct Message",
                            fontSize = 11.sp,
                            color = Color.White.copy(0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Outlined.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryPurple,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (input.isNotBlank()) {
                                vm.sendMessage(
                                    chatId = chatId,
                                    text = input,
                                    senderName = currentName,
                                    senderRole = currentRole
                                )
                                input = ""
                            }
                        },
                        containerColor = PrimaryPurple,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Outlined.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    ) { padding ->

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(BgGray)
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            if (messages.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No messages yet. Say hello! 👋", color = Color.Gray)
                    }
                }
            }

            items(messages, key = { it.messageId }) { msg ->
                MessageBubble(msg = msg, isMe = msg.senderId == currentUid)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean) {
    val isLecturer = msg.senderRole == "lecturer" || msg.senderRole == "admin"
    val bubbleColor = when {
        isMe -> PrimaryPurple
        isLecturer -> Color(0xFF2196F3)
        else -> Color.White
    }
    val textColor = if (isMe || isLecturer) Color.White else Color.Black

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isLecturer) Color(0xFF2196F3) else PrimaryPurple.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        msg.senderName.take(1).uppercase(),
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = msg.senderName + if (isLecturer) " 👨‍🏫" else "",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(2.dp))
        }

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isMe) 16.dp else 4.dp,
                        topEnd = if (isMe) 4.dp else 16.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(msg.text, color = textColor, fontSize = 14.sp)
        }

        Spacer(Modifier.height(4.dp))
    }
}