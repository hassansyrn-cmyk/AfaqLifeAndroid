package com.afaq.life

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var consentManager: AdConsentManager

    private lateinit var adContainer: FrameLayout
    private var adView: AdView? = null
    private var isAdRequestInProgress = false

    private var currentLanguage = "ar"
    private var lastBackPressTime = 0L

    fun updateLanguage(lang: String) {
        currentLanguage = lang
    }

    private fun handleExitFlow() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            finish()
        } else {
            lastBackPressTime = currentTime
            val msg = if (currentLanguage == "ar") {
                "اضغط مرة أخرى للخروج"
            } else {
                "Press again to exit"
            }
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor("#4A6760")
        window.navigationBarColor = Color.parseColor("#F8F5ED")

        notificationHelper = NotificationHelper(this)
        requestNotificationPermissionIfNeeded()

        consentManager = AdConsentManager(this)

        webView = WebView(this)
        webView.setBackgroundColor(Color.parseColor("#F8F5ED"))

        // Root container is a vertical LinearLayout to place WebView on top and Banner at the bottom.
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8F5ED"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Configure WebView layout parameters
        webView.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1.0f // Weight 1.0 to fill all available space on top of the banner
        )

        configureWebView()
        mainLayout.addView(webView)

        // Banner layout container at the bottom
        adContainer = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#F8F5ED"))
            visibility = View.GONE // GONE initially, hides space until loaded
        }
        mainLayout.addView(adContainer)

        setContentView(mainLayout)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (::webView.isInitialized) {
                    webView.evaluateJavascript("window.handleAndroidBackButton ? window.handleAndroidBackButton() : false") { result ->
                        val handled = result == "true"
                        if (!handled) {
                            handleExitFlow()
                        }
                    }
                } else {
                    handleExitFlow()
                }
            }
        })

        if (savedInstanceState == null) {
            webView.loadUrl(LOCAL_INDEX_URL)
        } else {
            webView.restoreState(savedInstanceState)
        }

        // Start Consent process and load Ads when allowed
        consentManager.gatherConsent(object : AdConsentManager.ConsentListener {
            override fun onConsentCompleted(canRequestAds: Boolean) {
                // Update JS UI about Privacy Options button requirements
                updatePrivacyButtonStateInJS()
                if (canRequestAds) {
                    loadBannerAd()
                }
            }
        })
    }

    /**
     * Determines the optimal anchored adaptive banner size according to screen width.
     */
    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val adWidthPixels = displayMetrics.widthPixels
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    /**
     * Loads the banner ad safely.
     */
    fun loadBannerAd() {
        if (isAdRequestInProgress) return
        isAdRequestInProgress = true

        runOnUiThread {
            try {
                // Destroy old ad if any exists to prevent memory leaks or duplicates
                adView?.destroy()
                adContainer.removeAllViews()

                val adUnitId = if (BuildConfig.DEBUG) {
                    "ca-app-pub-3940256099942544/9214589741" // Test Banner ID
                } else {
                    "ca-app-pub-7778383086464835/3618325111" // Production Banner ID
                }

                val newAdView = AdView(this).apply {
                    setAdSize(this@MainActivity.adSize)
                    setAdUnitId(adUnitId)
                }
                adView = newAdView

                newAdView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        isAdRequestInProgress = false
                        // Ad loaded successfully! Show the container
                        adContainer.visibility = View.VISIBLE
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        isAdRequestInProgress = false
                        Log.w("MainActivity", "Ad failed to load: ${loadAdError.message}")
                        // Hide container completely if failed
                        adContainer.visibility = View.GONE
                    }
                }

                adContainer.addView(newAdView)
                val adRequest = AdRequest.Builder().build()
                newAdView.loadAd(adRequest)
            } catch (e: Exception) {
                isAdRequestInProgress = false
                adContainer.visibility = View.GONE
                Log.e("MainActivity", "Exception loading Ad: ${e.message}", e)
            }
        }
    }

    /**
     * Informs WebView JavaScript interface about Privacy Options button requirements.
     */
    fun updatePrivacyButtonStateInJS() {
        runOnUiThread {
            if (::webView.isInitialized) {
                val isRequired = consentManager.isPrivacyOptionsRequired()
                // Calls the JS function to show/hide the privacy options settings entry point
                webView.evaluateJavascript("if (typeof updatePrivacyOptionsVisibility === 'function') { updatePrivacyOptionsVisibility($isRequired); }", null)
            }
        }
    }

    /**
     * Safely show Privacy Options form natively.
     */
    fun showPrivacyOptionsForm() {
        runOnUiThread {
            try {
                consentManager.showPrivacyOptionsForm { success ->
                    Log.d("MainActivity", "Privacy Options form completed with status: $success")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to display Privacy Options: ${e.message}", e)
            }
        }
    }

    /**
     * Debug method to reset consent for testing purposes.
     */
    fun resetConsentForTesting() {
        if (BuildConfig.DEBUG) {
            runOnUiThread {
                try {
                    consentManager.resetConsentForTesting()
                    // Re-run consent flow to refresh state
                    consentManager.gatherConsent(object : AdConsentManager.ConsentListener {
                        override fun onConsentCompleted(canRequestAds: Boolean) {
                            updatePrivacyButtonStateInJS()
                            if (canRequestAds) {
                                loadBannerAd()
                            } else {
                                adView?.destroy()
                                adContainer.visibility = View.GONE
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("MainActivity", "Consent reset failed: ${e.message}", e)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
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
            ): Boolean {
                return handleUrl(request.url)
            }

            @Deprecated("Kept for Android API 23 compatibility.")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrl(Uri.parse(url))
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Set the debug flag on JavaScript for conditional layouts/options
                val isDebug = BuildConfig.DEBUG
                webView.evaluateJavascript("window.isAfaqDebug = $isDebug;", null)
                // Ensure JavaScript UI is updated when the page finishes loading
                updatePrivacyButtonStateInJS()
            }
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
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_POST_NOTIFICATIONS
            )
        }
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroy() {
        adView?.destroy()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_POST_NOTIFICATIONS = 1001
        private const val LOCAL_WEB_BASE_URL = "file:///android_asset/web/"
        private const val LOCAL_INDEX_URL = "${LOCAL_WEB_BASE_URL}index.html"
    }
}
