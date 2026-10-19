package com.ahn.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themeUseCases.getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    init {
        recordAppOpen()
    }

    /** 인앱 리뷰 요청 조건에 쓰는 사용 일수를 남긴다. 같은 날 다시 불려도 하루로 센다. */
    private fun recordAppOpen() {
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
