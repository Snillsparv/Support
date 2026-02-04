package com.jonas.komihag.data

import androidx.lifecycle.LiveData
import androidx.room.*
import java.time.LocalDate

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE date = :date ORDER BY time ASC")
    fun getRemindersForDate(date: LocalDate): LiveData<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE date = :date ORDER BY time ASC")
    suspend fun getRemindersForDateSync(date: LocalDate): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY date ASC, time ASC")
    suspend fun getAllActiveReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Delete
    suspend fun delete(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE reminders SET isActive = :isActive WHERE id = :id")
    suspend fun setActive(id: Long, isActive: Boolean)

    @Query("DELETE FROM reminders WHERE date < :date")
    suspend fun deleteOldReminders(date: LocalDate)
}
