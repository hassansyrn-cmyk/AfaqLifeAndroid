package com.afaq.life

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class AfaqAndroid(
    private val context: Context,
    private val notificationHelper: NotificationHelper
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var focusTimer: CountDownTimer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val channelId = "afaq_focus_timer"
    private val notificationId = 1007

    init {
        createNotificationChannel()
    }

    @JavascriptInterface
    fun showNotification(title: String, body: String) {
        mainHandler.post {
            showNativeNotification(title, body)
        }
    }

    @JavascriptInterface
    fun startFocusTimer(minutes: Int) {
        mainHandler.post {
            val safeMinutes = minutes.coerceIn(1, 180)
            startNativeFocusTimer(safeMinutes)
        }
    }

    @JavascriptInterface
    fun stopFocusTimer() {
        mainHandler.post {
            stopNativeFocusTimer()
        }
    }

    private fun startNativeFocusTimer(minutes: Int) {
        stopNativeFocusTimer()
        acquireWakeLock(minutes)

        val totalMillis = minutes * 60 * 1000L

        showNativeNotification(
            "آفاق - مؤقت التركيز",
            "بدأت جلسة تركيز لمدة $minutes دقيقة"
        )

        focusTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                // يعمل المؤقت Native حتى لو انطفأت الشاشة.
            }

            override fun onFinish() {
                releaseWakeLock()
                playFinishSound()
                vibratePhone()

                showNativeNotification(
                    "انتهت جلسة التركيز",
                    "أحسنت! خذ استراحة قصيرة الآن."
                )
            }
        }.start()
    }

    private fun stopNativeFocusTimer() {
        focusTimer?.cancel()
        focusTimer = null
        releaseWakeLock()
    }

    private fun acquireWakeLock(minutes: Int) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Afaq:FocusTimerWakeLock"
            )

            val timeoutMillis = (minutes + 2) * 60 * 1000L
            wakeLock?.acquire(timeoutMillis)
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        } finally {
            wakeLock = null
        }
    }

    private fun showNativeNotification(title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()

        notificationManager.notify(notificationId, notification)
    }

    private fun playFinishSound() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone.play()
        } catch (_: Exception) {
        }
    }

    private fun vibratePhone() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0, 300, 120, 300, 120, 500),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 120, 300, 120, 500), -1)
            }
        } catch (_: Exception) {
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Afaq Focus Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Focus timer completion alerts"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }
}
