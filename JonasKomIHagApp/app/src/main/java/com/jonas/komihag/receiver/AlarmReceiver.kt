package com.jonas.komihag.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jonas.komihag.service.AlarmService
import com.jonas.komihag.ui.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val showNotification = intent.getBooleanExtra(EXTRA_SHOW_NOTIFICATION, true)
        val playAlarm = intent.getBooleanExtra(EXTRA_PLAY_ALARM, false)
        val speakMessage = intent.getBooleanExtra(EXTRA_SPEAK_MESSAGE, false)

        if (reminderId == -1L) return

        // Start the alarm service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_SHOW_NOTIFICATION, showNotification)
            putExtra(EXTRA_PLAY_ALARM, playAlarm)
            putExtra(EXTRA_SPEAK_MESSAGE, speakMessage)
        }
        context.startForegroundService(serviceIntent)

        // If alarm is enabled, show full-screen alarm activity
        if (playAlarm) {
            val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_SPEAK_MESSAGE, speakMessage)
            }
            context.startActivity(activityIntent)
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SHOW_NOTIFICATION = "show_notification"
        const val EXTRA_PLAY_ALARM = "play_alarm"
        const val EXTRA_SPEAK_MESSAGE = "speak_message"
    }
}
