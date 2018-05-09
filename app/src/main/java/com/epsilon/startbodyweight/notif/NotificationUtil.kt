package com.epsilon.startbodyweight.notif

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
import com.epsilon.startbodyweight.MainActivity
import com.epsilon.startbodyweight.R


private val NOTIFICATION_ID = 101
private val CHANNEL_ID = "com.epsilon.startbodyweight.notifchannel"

class NotificationUtil {


    companion object {
        @SuppressLint("NewApi")
        fun createNotification(c: Context, title: String, text: String) {
            val notificationManager = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "Workout notification channel", NotificationManager.IMPORTANCE_HIGH)
                channel.enableLights(true)
                channel.lightColor = Color.RED
                channel.setShowBadge(false)
                notificationManager.createNotificationChannel(channel)
            }

            val notificationBuilder = NotificationCompat.Builder(c, CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(android.R.drawable.btn_star)
                    .setDefaults(Notification.DEFAULT_SOUND)
                    .setAutoCancel(true)
                    .setContentIntent(getPendingIntentToLaunchActivity(c, MainActivity::class.java))
                    .addAction(getCompleteSetAction(c))
                    .addAction(getDismissAction(c))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                notificationBuilder.priority = Notification.PRIORITY_HIGH
            }

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }

        fun clearAllNotifications(c: Context?) {
            (c?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        }

        private fun getPendingIntentToLaunchActivity(c: Context, clazz: Class<*>): PendingIntent {
            val intent = Intent(c, clazz)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun getDismissAction(c: Context): NotificationCompat.Action {
            val intent = Intent(c.resources.getString(R.string.receiver_dismiss))
            val pendingIntent = PendingIntent.getBroadcast(c, 134134134, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action(R.drawable.ic_arrow_down_24dp, "Dismiss", pendingIntent)
        }

        private fun getCompleteSetAction(c: Context): NotificationCompat.Action {
            val intent = Intent(c.resources.getString(R.string.receiver_complete_set))
            val pendingIntent = PendingIntent.getBroadcast(c, 12345, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            return NotificationCompat.Action(R.drawable.ic_arrow_down_24dp, "Complete set", pendingIntent)
        }
    }
}
