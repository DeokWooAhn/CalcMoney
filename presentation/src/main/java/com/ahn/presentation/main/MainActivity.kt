package com.ahn.presentation.main

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.presentation.ads.AdConsentManager
import com.ahn.presentation.ads.AdConsentState
import com.ahn.presentation.ui.theme.CalcMoneyTheme
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adConsentManager: AdConsentManager
    private var adConsentState by mutableStateOf(AdConsentState())
    private var isMobileAdsInitialized = false
    private var isMobileAdsInitializing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adConsentManager = AdConsentManager(this)
        requestAdConsent()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CalcMoneyTheme(darkTheme = darkTheme) {
                MainScreen(
                    adConsentState = adConsentState,
                    onPrivacyOptionsClick = ::showPrivacyOptionsForm,
                )
            }
        }
    }

    private fun requestAdConsent() {
        adConsentManager.requestConsentInfo { state ->
            updateAdConsentState(state)
            initializeMobileAdsIfAllowed(state)
        }
    }

    private fun showPrivacyOptionsForm() {
        adConsentManager.showPrivacyOptionsForm { state ->
            updateAdConsentState(state)
            initializeMobileAdsIfAllowed(state)
        }
    }

    private fun updateAdConsentState(state: AdConsentState) {
        adConsentState = state.copy(
            canRequestAds = state.canRequestAds && isMobileAdsInitialized,
        )
    }

    private fun initializeMobileAdsIfAllowed(state: AdConsentState) {
        if (!state.canRequestAds || isMobileAdsInitialized || isMobileAdsInitializing) return

        isMobileAdsInitializing = true
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(this@MainActivity)
                withContext(Dispatchers.Main) {
                    val canRequestAds = adConsentManager.canRequestAds()
                    isMobileAdsInitialized = true
                    isMobileAdsInitializing = false
                    adConsentState = adConsentState.copy(canRequestAds = canRequestAds)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Mobile Ads SDK.", e)
                withContext(Dispatchers.Main) {
                    isMobileAdsInitializing = false
                }
            }
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}
