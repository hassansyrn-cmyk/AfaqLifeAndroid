package com.afaq.life

import android.app.Activity
import android.webkit.JavascriptInterface

class AfaqAndroid(
    private val activity: Activity,
    private val notificationHelper: NotificationHelper
) {
    @JavascriptInterface
    fun showNotification(title: String, body: String) {
        activity.runOnUiThread {
            notificationHelper.showNotification(title, body)
        }
    }
}
