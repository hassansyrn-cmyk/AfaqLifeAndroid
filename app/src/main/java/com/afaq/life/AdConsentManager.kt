package com.afaq.life

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Handles Google User Messaging Platform (UMP) consent flow and Google Mobile Ads SDK initialization safely.
 */
class AdConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    interface ConsentListener {
        fun onConsentCompleted(canRequestAds: Boolean)
    }

    /**
     * Checks consent status and gathers consent if required.
     */
    fun gatherConsent(listener: ConsentListener) {
        // Safe check for offline/failure scenarios. UMP is designed not to block the application.
        val debugSettingsBuilder = ConsentDebugSettings.Builder(activity)

        if (BuildConfig.DEBUG) {
            // Emulate EEA region in Debug
            debugSettingsBuilder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            // Automatically add the device test ID for UMP if needed.
            // Under normal debug run, UMP automatically registers the test device ID on the logs,
            // but we can add test device IDs if needed.
        }

        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            paramsBuilder.setConsentDebugSettings(debugSettingsBuilder.build())
        }

        val params = paramsBuilder.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info updated successfully, now check if we can load the form.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError ->
                    if (formError != null) {
                        Log.w("AdConsentManager", "Consent form display failed: ${formError.message}")
                    }
                    // Even if form fails or doesn't show, we check if we can request ads
                    val canRequestAds = canRequestAds()
                    if (canRequestAds) {
                        initializeMobileAds()
                    }
                    listener.onConsentCompleted(canRequestAds)
                }
            },
            { requestConsentError ->
                // Consent update failed (e.g. offline). We do NOT crash.
                Log.w("AdConsentManager", "Consent info update failed: ${requestConsentError.message}")
                val canRequestAds = canRequestAds()
                if (canRequestAds) {
                    initializeMobileAds()
                }
                listener.onConsentCompleted(canRequestAds)
            }
        )
    }

    /**
     * Resets consent status. ONLY available/functional in Debug mode for testing purposes.
     */
    fun resetConsentForTesting() {
        if (BuildConfig.DEBUG) {
            consentInformation.reset()
        }
    }

    /**
     * Returns whether the UMP SDK says we are allowed to request ads.
     */
    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    /**
     * Returns whether the Privacy Options Entry Point is required (user can change consent settings).
     */
    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * Triggers the Privacy Options form on the Main (UI) thread.
     */
    fun showPrivacyOptionsForm(onComplete: (Boolean) -> Unit) {
        activity.runOnUiThread {
            UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
                if (formError != null) {
                    Log.w("AdConsentManager", "Privacy Options Form failed: ${formError.message}")
                    onComplete(false)
                } else {
                    onComplete(true)
                }
            }
        }
    }

    private var isMobileAdsInitialized = false

    private fun initializeMobileAds() {
        if (isMobileAdsInitialized) return
        isMobileAdsInitialized = true
        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(activity) {}
    }
}
