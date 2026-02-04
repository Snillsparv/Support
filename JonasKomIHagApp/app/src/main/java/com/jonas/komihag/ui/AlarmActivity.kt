package com.jonas.komihag.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.jonas.komihag.databinding.ActivityAlarmBinding
import com.jonas.komihag.receiver.AlarmReceiver
import com.jonas.komihag.service.AlarmService
import java.util.Locale

class AlarmActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityAlarmBinding
    private var textToSpeech: TextToSpeech? = null
    private var message: String = ""
    private var reminderId: Long = -1
    private var shouldSpeak: Boolean = false
    private var ttsInitialized: Boolean = false

    private var characterAnimator: AnimatorSet? = null
    private var waveAnimator: ObjectAnimator? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize vibrator
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        // Get data from intent
        reminderId = intent.getLongExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1)
        message = intent.getStringExtra(AlarmReceiver.EXTRA_MESSAGE) ?: ""
        shouldSpeak = intent.getBooleanExtra(AlarmReceiver.EXTRA_SPEAK_MESSAGE, false)

        setupViews()
        startEntryAnimation()

        if (shouldSpeak) {
            textToSpeech = TextToSpeech(this, this)
        }
    }

    private fun setupViews() {
        binding.apply {
            tvMessage.text = message

            btnDismiss.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                stopAlarmAndFinish()
            }

            btnSnooze.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                snoozeAlarm()
            }
        }
    }

    private fun startEntryAnimation() {
        // Initial state
        binding.apply {
            characterContainer.alpha = 0f
            characterContainer.scaleX = 0.5f
            characterContainer.scaleY = 0.5f
            messageCard.alpha = 0f
            messageCard.translationY = 100f
            btnDismiss.alpha = 0f
            btnSnooze.alpha = 0f
        }

        // Entry animation sequence
        binding.characterContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                startCharacterAnimation()
            }
            .start()

        binding.messageCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.btnDismiss.animate()
            .alpha(1f)
            .setStartDelay(400)
            .setDuration(300)
            .start()

        binding.btnSnooze.animate()
            .alpha(1f)
            .setStartDelay(450)
            .setDuration(300)
            .start()
    }

    private fun startCharacterAnimation() {
        // Bounce animation for the character
        val scaleX = PropertyValuesHolder.ofFloat("scaleX", 1f, 1.15f, 1f)
        val scaleY = PropertyValuesHolder.ofFloat("scaleY", 1f, 1.15f, 1f)
        val translateY = PropertyValuesHolder.ofFloat("translationY", 0f, -25f, 0f)

        val bounceAnimator = ObjectAnimator.ofPropertyValuesHolder(
            binding.ivCharacter,
            scaleX, scaleY, translateY
        ).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Subtle rotation for more liveliness
        val rotateAnimator = ObjectAnimator.ofFloat(
            binding.ivCharacter, "rotation", -3f, 3f
        ).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
        }

        characterAnimator = AnimatorSet().apply {
            playTogether(bounceAnimator, rotateAnimator)
            start()
        }

        // Wave animation for the wave hand
        waveAnimator = ObjectAnimator.ofFloat(binding.ivWaveHand, "rotation", -25f, 25f).apply {
            duration = 350
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("sv", "SE"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.ENGLISH)
            }
            ttsInitialized = true

            // Speak the message with a greeting
            if (shouldSpeak && message.isNotEmpty()) {
                val fullMessage = "Hej! Dags för: $message"
                textToSpeech?.speak(fullMessage, TextToSpeech.QUEUE_FLUSH, null, "alarm_tts")
            }
        }
    }

    private fun snoozeAlarm() {
        // Stop current alarm
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        stopService(stopIntent)

        // Schedule new alarm in 5 minutes
        val alarmManager = getSystemService(AlarmManager::class.java)
        val snoozeIntent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(AlarmReceiver.EXTRA_MESSAGE, message)
            putExtra(AlarmReceiver.EXTRA_SHOW_NOTIFICATION, true)
            putExtra(AlarmReceiver.EXTRA_PLAY_ALARM, true)
            putExtra(AlarmReceiver.EXTRA_SPEAK_MESSAGE, shouldSpeak)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            (reminderId + 10000).toInt(), // Different request code for snooze
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
            pendingIntent
        )

        // Provide feedback
        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

        // Stop TTS and animations
        textToSpeech?.stop()
        characterAnimator?.cancel()
        waveAnimator?.cancel()

        finish()
    }

    private fun stopAlarmAndFinish() {
        // Stop the alarm service
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_DISMISS
        }
        stopService(stopIntent)

        // Stop TTS
        textToSpeech?.stop()

        // Stop animations
        characterAnimator?.cancel()
        waveAnimator?.cancel()

        // Confirmation vibration
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))

        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Require explicit dismiss - don't allow back button
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        characterAnimator?.cancel()
        waveAnimator?.cancel()
    }
}
