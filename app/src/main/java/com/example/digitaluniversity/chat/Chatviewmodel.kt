package com.example.digitaluniversity.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _conversations = MutableLiveData<List<ChatConversation>>()
    val conversations: LiveData<List<ChatConversation>> = _conversations

    // ── Load messages — sorted client-side to avoid needing a Firestore index ──
    fun loadMessages(chatId: String) {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener
                _messages.value = snap.documents
                    .mapNotNull { it.toObject(ChatMessage::class.java) }
                    .sortedBy { it.timestamp }  // sort client-side, no index needed
            }
    }

    // ── Send a message ──
    fun sendMessage(
        chatId: String,
        text: String,
        senderName: String,
        senderRole: String,
        onError: (String) -> Unit = {}
    ) {
        if (text.isBlank()) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val msgId = System.currentTimeMillis().toString()
        val msg = ChatMessage(
            messageId = msgId,
            senderId = uid,
            senderName = senderName,
            senderRole = senderRole,
            text = text.trim(),
            chatId = chatId
        )
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document(msgId)
            .set(msg)
            .addOnFailureListener { onError(it.message ?: "Send failed") }
    }

    // ── Load DM conversations for a course (lecturer view) ──
    fun loadLecturerConversations(courseId: String) {
        db.collection("chatMeta")
            .whereEqualTo("courseId", courseId)
            .whereEqualTo("type", "dm")
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                _conversations.value = snap.documents.mapNotNull {
                    it.toObject(ChatConversation::class.java)
                }
            }
    }

    // ── Resolve studentId from email, then open/create DM ──
    // This is the key fix: both student and lecturer resolve the same chatId
    // because it's always based on the student's UID
    fun openOrCreateDm(
        courseId: String,
        studentEmail: String,       // used by lecturer to look up studentId
        studentIdDirect: String,    // used by student directly (their own UID)
        studentName: String,
        lecturerId: String,
        lecturerName: String,
        onReady: (chatId: String, otherName: String) -> Unit
    ) {
        if (studentIdDirect.isNotBlank()) {
            // Student opening their own DM — UID is known directly
            val chatId = "dm_${courseId}_$studentIdDirect"
            ensureDmDoc(chatId, courseId, studentIdDirect, studentName, lecturerId, lecturerName) {
                onReady(chatId, lecturerName)
            }
        } else {
            // Lecturer opening DM — resolve student UID from email first
            db.collection("users")
                .whereEqualTo("email", studentEmail)
                .get()
                .addOnSuccessListener { snap ->
                    val studentDoc = snap.documents.firstOrNull()
                    val resolvedUid = studentDoc?.id ?: ""
                    val resolvedName = studentDoc?.getString("name") ?: studentEmail
                    if (resolvedUid.isBlank()) {
                        // Fallback: use email-safe key (won't match student side but better than crash)
                        val fallbackId = "dm_${courseId}_${studentEmail.replace(Regex("[^a-zA-Z0-9]"), "_")}"
                        onReady(fallbackId, resolvedName)
                        return@addOnSuccessListener
                    }
                    val chatId = "dm_${courseId}_$resolvedUid"
                    ensureDmDoc(chatId, courseId, resolvedUid, resolvedName, lecturerId, lecturerName) {
                        onReady(chatId, resolvedName)
                    }
                }
                .addOnFailureListener {
                    // Fallback if lookup fails
                    val fallbackId = "dm_${courseId}_${studentEmail.replace(Regex("[^a-zA-Z0-9]"), "_")}"
                    onReady(fallbackId, studentEmail)
                }
        }
    }

    private fun ensureDmDoc(
        chatId: String,
        courseId: String,
        studentId: String,
        studentName: String,
        lecturerId: String,
        lecturerName: String,
        onReady: () -> Unit
    ) {
        val ref = db.collection("chatMeta").document(chatId)
        ref.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val meta = ChatConversation(
                    chatId = chatId,
                    courseId = courseId,
                    type = "dm",
                    studentId = studentId,
                    studentName = studentName,
                    lecturerId = lecturerId,
                    lecturerName = lecturerName
                )
                ref.set(meta).addOnSuccessListener { onReady() }
                    .addOnFailureListener { onReady() } // proceed anyway
            } else {
                onReady()
            }
        }.addOnFailureListener { onReady() }
    }
}

data class ChatConversation(
    val chatId: String = "",
    val courseId: String = "",
    val type: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val lecturerId: String = "",
    val lecturerName: String = ""
)