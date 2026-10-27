package com.ahn.presentation.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * 전면 광고를 미리 받아 두었다가 보여 준다 (iOS `InterstitialAdPresenter` 대응).
 *
 * 보여 줄 때 광고를 받으려고 기다리게 하지 않도록 미리 받아 둔다. 보여 줄 차례인지는 정하지 않는다.
 * 그 판단은 `MainViewModel.shouldShowInterstitialAd`가 맡는다.
 */
class InterstitialAdController(
    private val activity: Activity,
    private val adUnitId: String,
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isShowing = false

    val isReady: Boolean
        get() = interstitialAd != null && !isShowing

    /** 받아 둔 광고가 없으면 하나 받아 둔다. 광고 동의와 SDK 초기화가 끝난 뒤에만 부른다. */
    fun preload() {
        if (interstitialAd != null || isLoading || isShowing) return

        isLoading = true
        InterstitialAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    Log.w(TAG, "Failed to load interstitial ad: ${error.message}")
                }
            },
        )
    }

    /**
     * 받아 둔 광고를 보여 준다. 광고가 화면에 뜨면 [onShown]을 부르고, 닫히면 다음 광고를 받아 둔다.
     *
     * 받아 둔 광고가 없으면 아무것도 하지 않는다.
     */
    fun show(onShown: () -> Unit) {
        val ad = interstitialAd ?: return

        interstitialAd = null
        isShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                onShown()
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                preload()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Failed to show interstitial ad: ${error.message}")
                isShowing = false
                preload()
            }
        }
        ad.show(activity)
    }

    private companion object {
        const val TAG = "InterstitialAdController"
    }
}
