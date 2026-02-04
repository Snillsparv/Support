package com.jonas.komihag.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.jonas.komihag.ReminderApplication
import com.jonas.komihag.data.Reminder
import com.jonas.komihag.data.TimeSlot
import com.jonas.komihag.util.AlarmScheduler
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ReminderApplication).repository

    private val _selectedDate = MutableLiveData(LocalDate.now())
    val selectedDate: LiveData<LocalDate> = _selectedDate

    val remindersForDate: LiveData<List<Reminder>> = _selectedDate.switchMap { date ->
        repository.getRemindersForDate(date)
    }

    private val _timeSlots = MutableLiveData<List<TimeSlot>>()
    val timeSlots: LiveData<List<TimeSlot>> = _timeSlots

    init {
        _timeSlots.value = TimeSlot.generateDaySlots()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun updateTimeSlotsWithReminders(reminders: List<Reminder>) {
        val slots = TimeSlot.generateDaySlots()
        reminders.forEach { reminder ->
            val slotIndex = reminder.getTimeSlotIndex()
            if (slotIndex in slots.indices) {
                slots[slotIndex].reminder = reminder
            }
        }
        _timeSlots.value = slots
    }

    fun saveReminder(reminder: Reminder) {
        viewModelScope.launch {
            val id = repository.insert(reminder)
            val savedReminder = reminder.copy(id = id)
            AlarmScheduler.scheduleAlarm(getApplication(), savedReminder)
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.update(reminder)
            AlarmScheduler.cancelAlarm(getApplication(), reminder.id)
            if (reminder.isActive) {
                AlarmScheduler.scheduleAlarm(getApplication(), reminder)
            }
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.delete(reminder)
            AlarmScheduler.cancelAlarm(getApplication(), reminder.id)
        }
    }

    fun goToNextDay() {
        _selectedDate.value = _selectedDate.value?.plusDays(1)
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value?.minusDays(1)
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }
}
