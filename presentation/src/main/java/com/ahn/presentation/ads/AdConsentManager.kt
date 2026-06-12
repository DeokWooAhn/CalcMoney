package com.ahn.presentation.ads

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

data class AdConsentState(
    val canRequestAds: Boolean = false,
    val isPrivacyOptionsRequired: Boolean = false,
)

class AdConsentManager(private val activity: Activity) {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

    fun requestConsentInfo(onResult: (AdConsentState) -> Unit) {
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    formError?.let { error ->
                        Log.w(TAG, "Failed to load or show consent form: ${error.message}")
                    }
                    onResult(currentState())
                }
            },
            { requestError ->
                Log.w(TAG, "Failed to update consent information: ${requestError.message}")
                onResult(currentState())
            },
        )
    }

    fun showPrivacyOptionsForm(onResult: (AdConsentState) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let { error ->
                Log.w(TAG, "Failed to show privacy options form: ${error.message}")
            }
            onResult(currentState())
        }
    }

    fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    private fun currentState(): AdConsentState {
        return AdConsentState(
            canRequestAds = consentInformation.canRequestAds(),
            isPrivacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
        )
    }

    private companion object {
        const val TAG = "AdConsentManager"
    }
}
