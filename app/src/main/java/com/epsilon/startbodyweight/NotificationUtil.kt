package com.epsilon.startbodyweight

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat


class NotificationUtil {
    companion object {
        @SuppressLint("NewApi")
        fun createNotification(c: Context, title: String, text: String) {
            val notificationID = 101
            val channelID = "com.ebookfrenzy.notifydemo.news"

            val notificationManager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelID, "Workout notification channel", NotificationManager.IMPORTANCE_HIGH)
                channel.description = "Rest timer channel"
                channel.enableLights(true)
                channel.lightColor = Color.RED
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(c, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(c, 0 /* Request code */, intent, PendingIntent.FLAG_ONE_SHOT)
            val notification = NotificationCompat.Builder(c, channelID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelID)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build()

            notificationManager.notify(notificationID, notification)
        }
    }
}
