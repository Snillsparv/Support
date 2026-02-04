package com.jonas.komihag.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jonas.komihag.R
import com.jonas.komihag.data.Reminder
import com.jonas.komihag.data.TimeSlot
import com.jonas.komihag.databinding.ActivityMainBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: TimeSlotAdapter

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupDateNavigation()
        observeViewModel()
        checkPermissions()
    }

    private fun setupRecyclerView() {
        adapter = TimeSlotAdapter(
            onSlotClick = { timeSlot -> showReminderDialog(timeSlot) },
            onSlotLongClick = { timeSlot ->
                if (timeSlot.hasReminder) {
                    showDeleteDialog(timeSlot)
                    true
                } else {
                    false
                }
            }
        )

        binding.rvTimeSlots.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }

        // Scroll to current time
        scrollToCurrentTime()
    }

    private fun scrollToCurrentTime() {
        val currentSlotIndex = java.time.LocalTime.now().let { time ->
            time.hour * 2 + if (time.minute >= 30) 1 else 0
        }
        binding.rvTimeSlots.scrollToPosition(maxOf(0, currentSlotIndex - 2))
    }

    private fun setupDateNavigation() {
        binding.apply {
            btnPrevDay.setOnClickListener { viewModel.goToPreviousDay() }
            btnNextDay.setOnClickListener { viewModel.goToNextDay() }
            btnToday.setOnClickListener {
                viewModel.goToToday()
                scrollToCurrentTime()
            }

            tvDate.setOnClickListener { showDatePicker() }
        }
    }

    private fun showDatePicker() {
        val currentDate = viewModel.selectedDate.value ?: LocalDate.now()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                viewModel.setSelectedDate(LocalDate.of(year, month + 1, day))
            },
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        ).show()
    }

    private fun observeViewModel() {
        viewModel.selectedDate.observe(this) { date ->
            updateDateDisplay(date)
        }

        viewModel.remindersForDate.observe(this) { reminders ->
            viewModel.updateTimeSlotsWithReminders(reminders)
        }

        viewModel.timeSlots.observe(this) { slots ->
            adapter.submitList(slots.toList())
        }
    }

    private fun updateDateDisplay(date: LocalDate) {
        val today = LocalDate.now()
        val dateText = when (date) {
            today -> "Idag"
            today.plusDays(1) -> "Imorgon"
            today.minusDays(1) -> "Igår"
            else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("sv", "SE")))
        }
        binding.tvDate.text = dateText

        val fullDate = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("sv", "SE")))
        binding.tvFullDate.text = fullDate

        // Show/hide today button
        binding.btnToday.visibility = if (date == today) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    private fun showReminderDialog(timeSlot: TimeSlot) {
        val dialog = ReminderDialogFragment.newInstance(
            date = viewModel.selectedDate.value ?: LocalDate.now(),
            timeSlot = timeSlot,
            existingReminder = timeSlot.reminder
        )
        dialog.setOnSaveListener { reminder ->
            if (timeSlot.hasReminder) {
                viewModel.updateReminder(reminder)
            } else {
                viewModel.saveReminder(reminder)
            }
        }
        dialog.show(supportFragmentManager, "reminder_dialog")
    }

    private fun showDeleteDialog(timeSlot: TimeSlot) {
        timeSlot.reminder?.let { reminder ->
            AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Ta bort påminnelse")
                .setMessage("Vill du ta bort påminnelsen \"${reminder.message}\"?")
                .setPositiveButton("Ta bort") { _, _ ->
                    viewModel.deleteReminder(reminder)
                }
                .setNegativeButton("Avbryt", null)
                .show()
        }
    }

    private fun checkPermissions() {
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showNotificationPermissionRationale()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // Check exact alarm permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                showExactAlarmPermissionDialog()
            }
        }
    }

    private fun showNotificationPermissionRationale() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Notifikationer behövs")
            .setMessage("Appen behöver skicka notifikationer för att påminna dig om dina uppgifter.")
            .setPositiveButton("OK") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Avbryt", null)
            .show()
    }

    private fun showExactAlarmPermissionDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Alarmbehörighet behövs")
            .setMessage("För att alarmen ska fungera exakt på utsatt tid behöver appen behörighet att schemalägga exakta alarm.")
            .setPositiveButton("Öppna inställningar") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    })
                }
            }
            .setNegativeButton("Avbryt", null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogTheme)
            .setTitle("Behörighet nekad")
            .setMessage("Utan notifikationsbehörighet kommer påminnelser inte att visas.")
            .setPositiveButton("OK", null)
            .show()
    }
}
