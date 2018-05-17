package com.epsilon.startbodyweight.notif

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.epsilon.startbodyweight.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
        val action = intent?.action
        when {
            action.equals(c?.resources?.getString(R.string.action_complete_set)) -> {
                Toast.makeText(c, "Set completed. [TODO]", Toast.LENGTH_SHORT).show()
                NotificationUtil.clearAllNotifications(c)
            }
            action.equals(c?.resources?.getString(R.string.action_dismiss)) -> {
                NotificationUtil.clearAllNotifications(c)
            }
            action.equals(c?.resources?.getString(R.string.action_display_notif)) -> {
                val notificationManager = c?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notification = intent?.getParcelableExtra(c.resources.getString(R.string.NOTIFICATION_TAG)) as Notification
                val id = intent.getIntExtra(c.resources.getString(R.string.NOTIFICATION_ID_TAG), 0)

                // Displays the notification that was attached in our pending intent
                notificationManager.notify(id, notification)
            }
        }
    }

}