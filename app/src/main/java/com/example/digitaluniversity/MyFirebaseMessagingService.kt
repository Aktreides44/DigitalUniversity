package com.example.digitaluniversity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {



    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val uid =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update(
                mapOf(
                    "fcmToken" to token
                )
            )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(
            "FCM_DEBUG",
            "MESSAGE RECEIVED: ${message.data}"
        )


        super.onMessageReceived(message)

        val title =
            message.notification?.title ?: "Digital University"

        val body =
            message.notification?.body ?: ""

        showNotification(title, body)
    }

    private fun showNotification(
        title: String,
        body: String
    ) {

        val channelId = "digital_uni"

        val manager =
            getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    channelId,
                    "Digital University",
                    NotificationManager.IMPORTANCE_HIGH
                )

            manager.createNotificationChannel(channel)
        }

        val notification =
            NotificationCompat.Builder(
                this,
                channelId
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .build()

        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}