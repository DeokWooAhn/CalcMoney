package com.ahn.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahn.domain.ads.usecase.InterstitialAdUseCases
import com.ahn.domain.review.usecase.ReviewPromptUseCases
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.domain.setting.usecase.ThemeUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val themeUseCases: ThemeUseCases,
    private val reviewPromptUseCases: ReviewPromptUseCases,
    private val interstitialAdUseCases: InterstitialAdUseCases,
) : ViewModel() {
    /** 이번 실행에서 인앱 리뷰를 요청했는지. 리뷰를 부탁한 직후에는 전면 광고를 띄우지 않는다. */
    private var hasRequestedReviewThisSession = false

    val themeMode: StateFlow<ThemeMode> = themeUseCases.getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    /**
     * 인앱 리뷰 요청 조건에 쓰는 사용 일수를 남긴다. 같은 날 다시 불려도 하루로 센다.
     *
     * 앱이 앞으로 올 때마다(`MainActivity.onStart`) 부른다. Activity가 살아 있는 채로 며칠 뒤 돌아와도 그날을 센다.
     */
    fun recordAppOpen() {
        viewModelScope.launch {
            try {
                reviewPromptUseCases.recordAppOpen()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to record app open.", e)
            }
        }
    }

    /** 인앱 리뷰를 요청했다고 남긴다. 이번 실행에서는 전면 광고를 띄우지 않는다. */
    fun markReviewRequested() {
        hasRequestedReviewThisSession = true
    }

    /**
     * 계산기를 떠나는 지금 전면 광고를 보여 줄 차례인지 알려 준다.
     *
     * 기록을 읽지 못하면 광고를 건너뛴다. 광고를 실제로 보여 줬으면 [recordInterstitialAdShown]을 부른다.
     */
    suspend fun shouldShowInterstitialAd(): Boolean {
        if (hasRequestedReviewThisSession) return false

        return try {
            interstitialAdUseCases.shouldShow()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("MainViewModel", "Failed to check interstitial ad record.", e)
            false
        }
    }

    fun recordInterstitialAdShown() {
        viewModelScope.launch {
            try {
                interstitialAdUseCases.recordShown()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to record interstitial ad shown.", e)
            }
        }
    }

    fun saveThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            try {
                themeUseCases.saveThemeMode(themeMode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MainViewModel", "Failed to save theme mode.", e)
            }
        }
    }
}
