package com.afaq.life

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "آفاق"
        val body = intent.getStringExtra("body") ?: ""

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(title, body)
    }
}
