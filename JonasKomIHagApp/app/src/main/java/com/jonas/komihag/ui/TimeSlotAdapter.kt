package com.jonas.komihag.ui

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jonas.komihag.R
import com.jonas.komihag.data.TimeSlot
import com.jonas.komihag.databinding.ItemTimeSlotBinding
import java.time.LocalTime

class TimeSlotAdapter(
    private val onSlotClick: (TimeSlot) -> Unit,
    private val onSlotLongClick: (TimeSlot) -> Boolean
) : ListAdapter<TimeSlot, TimeSlotAdapter.TimeSlotViewHolder>(TimeSlotDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeSlotViewHolder {
        val binding = ItemTimeSlotBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TimeSlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeSlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeSlotViewHolder(
        private val binding: ItemTimeSlotBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timeSlot: TimeSlot) {
            binding.apply {
                tvTime.text = timeSlot.timeLabel

                val currentTime = LocalTime.now()
                val isCurrentSlot = currentTime >= timeSlot.startTime && currentTime < timeSlot.endTime
                val isPastSlot = timeSlot.endTime < currentTime

                if (timeSlot.hasReminder) {
                    val reminder = timeSlot.reminder!!

                    // Show reminder message
                    tvReminder.text = reminder.message
                    tvReminder.visibility = View.VISIBLE
                    tvEmptyHint.visibility = View.GONE

                    // Background color
                    cardTimeSlot.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.slot_with_reminder)
                    )

                    // Show indicators
                    ivNotification.visibility = if (reminder.showNotification) View.VISIBLE else View.GONE
                    ivAlarm.visibility = if (reminder.playAlarm) View.VISIBLE else View.GONE
                    ivSpeaker.visibility = if (reminder.speakMessage) View.VISIBLE else View.GONE

                    // Text color based on time
                    val textColor = if (isPastSlot) R.color.text_hint else R.color.text_primary
                    tvReminder.setTextColor(ContextCompat.getColor(root.context, textColor))

                } else {
                    // No reminder
                    tvReminder.visibility = View.GONE
                    ivNotification.visibility = View.GONE
                    ivAlarm.visibility = View.GONE
                    ivSpeaker.visibility = View.GONE

                    // Show hint for current/future slots
                    if (!isPastSlot) {
                        tvEmptyHint.visibility = View.VISIBLE
                    } else {
                        tvEmptyHint.visibility = View.GONE
                    }

                    // Background color
                    val bgColor = when {
                        isCurrentSlot -> R.color.slot_current
                        isPastSlot -> R.color.slot_past
                        else -> R.color.slot_empty
                    }
                    cardTimeSlot.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, bgColor)
                    )
                }

                // Time text style based on current time
                val timeTextColor = when {
                    isCurrentSlot -> R.color.primary
                    isPastSlot -> R.color.text_hint
                    else -> R.color.text_primary
                }
                tvTime.setTextColor(ContextCompat.getColor(root.context, timeTextColor))

                // Current time indicator
                currentTimeIndicator.visibility = if (isCurrentSlot) View.VISIBLE else View.GONE

                // Divider alpha for past slots
                divider.alpha = if (isPastSlot) 0.3f else 1f

                // Click listeners
                root.setOnClickListener { view ->
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onSlotClick(timeSlot)
                }
                root.setOnLongClickListener { view ->
                    if (timeSlot.hasReminder) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onSlotLongClick(timeSlot)
                    } else {
                        false
                    }
                }
            }
        }
    }

    class TimeSlotDiffCallback : DiffUtil.ItemCallback<TimeSlot>() {
        override fun areItemsTheSame(oldItem: TimeSlot, newItem: TimeSlot): Boolean {
            return oldItem.index == newItem.index
        }

        override fun areContentsTheSame(oldItem: TimeSlot, newItem: TimeSlot): Boolean {
            return oldItem == newItem && oldItem.reminder == newItem.reminder
        }
    }
}
