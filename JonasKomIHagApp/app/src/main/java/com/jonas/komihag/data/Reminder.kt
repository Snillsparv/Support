package com.jonas.komihag.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
    val message: String,
    val showNotification: Boolean = true,
    val playAlarm: Boolean = false,
    val speakMessage: Boolean = false,
    val isActive: Boolean = true
) {
    fun getTimeSlotIndex(): Int {
        return time.hour * 2 + if (time.minute >= 30) 1 else 0
    }

    companion object {
        fun timeFromSlotIndex(index: Int): LocalTime {
            val hour = index / 2
            val minute = if (index % 2 == 0) 0 else 30
            return LocalTime.of(hour, minute)
        }
    }
}
