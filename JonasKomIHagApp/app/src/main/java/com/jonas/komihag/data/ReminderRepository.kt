package com.jonas.komihag.data

import androidx.lifecycle.LiveData
import java.time.LocalDate

class ReminderRepository(private val reminderDao: ReminderDao) {

    fun getRemindersForDate(date: LocalDate): LiveData<List<Reminder>> {
        return reminderDao.getRemindersForDate(date)
    }

    suspend fun getRemindersForDateSync(date: LocalDate): List<Reminder> {
        return reminderDao.getRemindersForDateSync(date)
    }

    suspend fun getAllActiveReminders(): List<Reminder> {
        return reminderDao.getAllActiveReminders()
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)
    }

    suspend fun insert(reminder: Reminder): Long {
        return reminderDao.insert(reminder)
    }

    suspend fun update(reminder: Reminder) {
        reminderDao.update(reminder)
    }

    suspend fun delete(reminder: Reminder) {
        reminderDao.delete(reminder)
    }

    suspend fun deleteById(id: Long) {
        reminderDao.deleteById(id)
    }

    suspend fun setActive(id: Long, isActive: Boolean) {
        reminderDao.setActive(id, isActive)
    }

    suspend fun deleteOldReminders(date: LocalDate) {
        reminderDao.deleteOldReminders(date)
    }
}
