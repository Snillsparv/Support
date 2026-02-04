package com.jonas.komihag.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jonas.komihag.ReminderApplication
import com.jonas.komihag.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as ReminderApplication

            CoroutineScope(Dispatchers.IO).launch {
                val reminders = app.repository.getAllActiveReminders()
                AlarmScheduler.rescheduleAllAlarms(context, reminders)
            }
        }
    }
}
