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

import android.app.PendingIntent
import android.content.Intent

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

    @JavascriptInterface
    fun scheduleNotification(title: String, body: String, delaySeconds: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    @JavascriptInterface
    fun scheduleRepeatingReminder(hour: Int, minute: Int, title: String, body: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("body", body)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            android.app.AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            android.app.AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    @JavascriptInterface
    fun cancelRepeatingReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    @JavascriptInterface
    fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    @JavascriptInterface
    fun requestNotificationPermission() {
        mainHandler.post {
            if (context is android.app.Activity) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
            }
        }
    }

    @JavascriptInterface
    fun exitApp() {
        mainHandler.post {
            if (context is android.app.Activity) {
                context.finish()
            }
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
                // Native timer keeps running while the screen is off.
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
