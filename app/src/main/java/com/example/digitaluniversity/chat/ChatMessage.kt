package com.example.digitaluniversity.chat

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "",
    val text: String = "",
    val timestamp: com.google.firebase.Timestamp = com.google.firebase.Timestamp.now(),
    val chatId: String = ""
)