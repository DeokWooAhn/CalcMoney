package com.ahn.presentation.main

import android.content.pm.ApplicationInfo
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
        // UI 테스트가 UMP의 지역 판정에 좌우되면 안 된다. EEA로 판정되면 동의 양식이 화면을 덮어
        // 테스트가 통째로 막힌다. 동의 흐름을 건너뛰면 canRequestAds가 false로 남아 광고 로드도
        // 함께 빠지므로, 테스트가 실제 광고를 노출시키지도 않는다. iOS AdConsentManager와 같은 동작.
        if (isUiTesting()) {
            Log.i(TAG, "UI testing detected; skipping ad consent flow")
        } else {
            requestAdConsent()
        }

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

    /**
     * 디버그 빌드에 [UI_TESTING_EXTRA]가 붙어 실행됐는지 확인한다.
     *
     * 런처 Activity는 exported라 다른 앱도 extra를 붙여 실행할 수 있다. 릴리스 빌드에서 이 값을
     * 받아 주면 동의 흐름을 외부에서 끌 수 있게 되므로 디버그 빌드로 한정한다.
     */
    private fun isUiTesting(): Boolean {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return isDebuggable && intent.getBooleanExtra(UI_TESTING_EXTRA, false)
    }

    private companion object {
        const val TAG = "MainActivity"

        /** Maestro Flow의 `launchApp.arguments`가 넘기는 값. iOS `AdConsentManager.uiTestingLaunchArgument` 대응. */
        const val UI_TESTING_EXTRA = "UI_TESTING"
    }
}
