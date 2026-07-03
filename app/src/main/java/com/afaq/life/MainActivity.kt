package com.afaq.life

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#4A6760")
        window.navigationBarColor = Color.parseColor("#F8F5ED")

        notificationHelper = NotificationHelper(this)
        requestNotificationPermissionIfNeeded()

        webView = WebView(this)
        webView.setBackgroundColor(Color.parseColor("#F8F5ED"))
        webView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        configureWebView()

        val container = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F8F5ED"))
            addView(webView)
        }
        setContentView(container)

        if (savedInstanceState == null) {
            webView.loadUrl(LOCAL_INDEX_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun configureWebView() {
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadsImagesAutomatically = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }
        }

        webView.addJavascriptInterface(
            AfaqAndroid(this, notificationHelper),
            "AfaqAndroid"
        )

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean = handleUrl(request.url)

            @Deprecated("Kept for Android API 23 compatibility.")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                handleUrl(Uri.parse(url))
        }
    }

    private fun handleUrl(uri: Uri): Boolean {
        val url = uri.toString()
        val isLocalWebAsset =
            url.startsWith(LOCAL_WEB_BASE_URL) ||
                (uri.scheme == null && (url == "privacy.html" || url == "index.html"))

        return if (isLocalWebAsset) {
            false
        } else {
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
            true
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1001
        private const val LOCAL_WEB_BASE_URL = "file:///android_asset/web/"
        private const val LOCAL_INDEX_URL = "${LOCAL_WEB_BASE_URL}index.html"
    }
}
