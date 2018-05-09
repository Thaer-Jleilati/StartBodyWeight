package com.epsilon.startbodyweight.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.epsilon.startbodyweight.R


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context?, intent: Intent?) {
        val action = intent?.action
        when {
            action.equals(c?.resources?.getString(R.string.receiver_complete_set)) -> {
                Toast.makeText(c, "YES CALLED", Toast.LENGTH_SHORT).show()
                NotificationUtil.clearAllNotifications(c)
            }
            action.equals(c?.resources?.getString(R.string.receiver_dismiss)) -> NotificationUtil.clearAllNotifications(c)
        }
    }

}