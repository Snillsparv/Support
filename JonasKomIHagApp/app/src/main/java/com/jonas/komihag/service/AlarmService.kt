package com.jonas.komihag.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.jonas.komihag.R
import com.jonas.komihag.ReminderApplication
import com.jonas.komihag.receiver.AlarmReceiver
import com.jonas.komihag.ui.AlarmActivity
import com.jonas.komihag.ui.MainActivity
import java.util.Locale

class AlarmService : Service(), TextToSpeech.OnInitListener {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var textToSpeech: TextToSpeech? = null
    private var pendingMessage: String? = null
    private var shouldSpeak: Boolean = false
    private var ttsReady: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getLongExtra(AlarmReceiver.EXTRA_REMINDER_ID, -1) ?: -1
        val message = intent?.getStringExtra(AlarmReceiver.EXTRA_MESSAGE) ?: ""
        val showNotification = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SHOW_NOTIFICATION, true) ?: true
        val playAlarm = intent?.getBooleanExtra(AlarmReceiver.EXTRA_PLAY_ALARM, false) ?: false
        val speakMessage = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SPEAK_MESSAGE, false) ?: false

        shouldSpeak = speakMessage
        pendingMessage = message

        // Start as foreground service with notification
        val notification = createNotification(message, reminderId, playAlarm)
        startForeground(NOTIFICATION_ID, notification)

        // Play alarm sound if enabled
        if (playAlarm) {
            playAlarmSound()
            startVibration()
        }

        // Speak message if TTS is ready
        if (speakMessage && ttsReady) {
            speakText(message)
        }

        return START_NOT_STICKY
    }

    private fun createNotification(message: String, reminderId: Long, isAlarm: Boolean): Notification {
        val channelId = if (isAlarm) {
            ReminderApplication.CHANNEL_ALARM
        } else {
            ReminderApplication.CHANNEL_NOTIFICATION
        }

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this, 1, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Jonas kom-i-håg-app!")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_dismiss, "Avfärda", dismissPendingIntent)

        if (isAlarm) {
            val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
                putExtra(AlarmReceiver.EXTRA_MESSAGE, message)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this, 2, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        return builder.build()
    }

    private fun playAlarmSound() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    this@AlarmService,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun speakText(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reminder_tts")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("sv", "SE"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.ENGLISH)
            }
            ttsReady = true

            // Speak pending message if needed
            if (shouldSpeak && !pendingMessage.isNullOrEmpty()) {
                speakText(pendingMessage!!)
            }
        }
    }

    fun stopAlarm() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        textToSpeech?.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.cancel()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_DISMISS = "com.jonas.komihag.ACTION_DISMISS"
    }
}
