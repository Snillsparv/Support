package com.jonas.komihag.data

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class TimeSlot(
    val index: Int,
    val startTime: LocalTime,
    val endTime: LocalTime,
    var reminder: Reminder? = null
) {
    val timeLabel: String
        get() = startTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    val hasReminder: Boolean
        get() = reminder != null

    companion object {
        fun generateDaySlots(): List<TimeSlot> {
            return (0 until 48).map { index ->
                val startHour = index / 2
                val startMinute = if (index % 2 == 0) 0 else 30
                val startTime = LocalTime.of(startHour, startMinute)

                val endIndex = index + 1
                val endHour = endIndex / 2
                val endMinute = if (endIndex % 2 == 0) 0 else 30
                val endTime = if (endIndex >= 48) LocalTime.of(23, 59) else LocalTime.of(endHour, endMinute)

                TimeSlot(index, startTime, endTime)
            }
        }
    }
}
