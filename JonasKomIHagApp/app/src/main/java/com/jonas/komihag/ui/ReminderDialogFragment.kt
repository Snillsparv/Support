package com.jonas.komihag.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.jonas.komihag.R
import com.jonas.komihag.data.Reminder
import com.jonas.komihag.data.TimeSlot
import com.jonas.komihag.databinding.DialogReminderBinding
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderDialogFragment : DialogFragment() {

    private var _binding: DialogReminderBinding? = null
    private val binding get() = _binding!!

    private var onSaveListener: ((Reminder) -> Unit)? = null
    private var existingReminder: Reminder? = null
    private lateinit var date: LocalDate
    private lateinit var time: LocalTime

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullWidthDialog)

        arguments?.let { args ->
            date = LocalDate.parse(args.getString(ARG_DATE))
            time = LocalTime.parse(args.getString(ARG_TIME))
            args.getString(ARG_EXISTING_REMINDER)?.let { reminderJson ->
                // Parse existing reminder if provided
                val parts = reminderJson.split("|")
                if (parts.size >= 6) {
                    existingReminder = Reminder(
                        id = parts[0].toLong(),
                        date = LocalDate.parse(parts[1]),
                        time = LocalTime.parse(parts[2]),
                        message = parts[3],
                        showNotification = parts[4].toBoolean(),
                        playAlarm = parts[5].toBoolean(),
                        speakMessage = parts[6].toBoolean()
                    )
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupViews() {
        binding.apply {
            // Display time
            tvTimeSlot.text = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            tvDateDisplay.text = date.format(
                DateTimeFormatter.ofPattern("EEEE d MMMM", Locale("sv", "SE"))
            )

            // Set title based on edit or new
            tvTitle.text = if (existingReminder != null) {
                getString(R.string.edit_reminder)
            } else {
                getString(R.string.new_reminder)
            }

            // Fill in existing data if editing
            existingReminder?.let { reminder ->
                etMessage.setText(reminder.message)
                cbNotification.isChecked = reminder.showNotification
                cbAlarm.isChecked = reminder.playAlarm
                cbSpeak.isChecked = reminder.speakMessage
            }

            // Focus on text field
            etMessage.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.apply {
            btnSave.setOnClickListener {
                saveReminder()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }

            // Auto-check notification if alarm is checked
            cbAlarm.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    cbNotification.isChecked = true
                }
            }
        }
    }

    private fun saveReminder() {
        val message = binding.etMessage.text.toString().trim()

        if (message.isEmpty()) {
            binding.tilMessage.error = "Skriv vad du vill bli påmind om"
            return
        }

        val reminder = Reminder(
            id = existingReminder?.id ?: 0,
            date = date,
            time = time,
            message = message,
            showNotification = binding.cbNotification.isChecked,
            playAlarm = binding.cbAlarm.isChecked,
            speakMessage = binding.cbSpeak.isChecked
        )

        onSaveListener?.invoke(reminder)
        dismiss()
    }

    fun setOnSaveListener(listener: (Reminder) -> Unit) {
        onSaveListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DATE = "arg_date"
        private const val ARG_TIME = "arg_time"
        private const val ARG_EXISTING_REMINDER = "arg_existing_reminder"

        fun newInstance(
            date: LocalDate,
            timeSlot: TimeSlot,
            existingReminder: Reminder?
        ): ReminderDialogFragment {
            return ReminderDialogFragment().apply {
                arguments = bundleOf(
                    ARG_DATE to date.toString(),
                    ARG_TIME to timeSlot.startTime.toString(),
                    ARG_EXISTING_REMINDER to existingReminder?.let { r ->
                        "${r.id}|${r.date}|${r.time}|${r.message}|${r.showNotification}|${r.playAlarm}|${r.speakMessage}"
                    }
                )
            }
        }
    }
}
