package com.jonas.komihag.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jonas.komihag.data.Reminder
import com.jonas.komihag.receiver.AlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId

object AlarmScheduler {

    fun scheduleAlarm(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, reminder.message)
            putExtra(AlarmReceiver.EXTRA_SHOW_NOTIFICATION, reminder.showNotification)
            putExtra(AlarmReceiver.EXTRA_PLAY_ALARM, reminder.playAlarm)
            putExtra(AlarmReceiver.EXTRA_SPEAK_MESSAGE, reminder.speakMessage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = LocalDateTime.of(reminder.date, reminder.time)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Only schedule if the time is in the future
        if (triggerTime > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                    pendingIntent
                )
            }
        }
    }

    fun cancelAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAllAlarms(context: Context, reminders: List<Reminder>) {
        reminders.forEach { reminder ->
            if (reminder.isActive) {
                scheduleAlarm(context, reminder)
            }
        }
    }
}
