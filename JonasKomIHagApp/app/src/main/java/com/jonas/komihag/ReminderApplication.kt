package com.jonas.komihag

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.jonas.komihag.data.ReminderDatabase
import com.jonas.komihag.data.ReminderRepository

class ReminderApplication : Application() {

    val database by lazy { ReminderDatabase.getDatabase(this) }
    val repository by lazy { ReminderRepository(database.reminderDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Standard notification channel
        val standardChannel = NotificationChannel(
            CHANNEL_NOTIFICATION,
            "Påminnelser",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikationer för påminnelser"
            enableVibration(true)
        }

        // Alarm channel with sound
        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarmljud för påminnelser"
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        notificationManager.createNotificationChannels(listOf(standardChannel, alarmChannel))
    }

    companion object {
        const val CHANNEL_NOTIFICATION = "reminder_notification"
        const val CHANNEL_ALARM = "reminder_alarm"
    }
}
