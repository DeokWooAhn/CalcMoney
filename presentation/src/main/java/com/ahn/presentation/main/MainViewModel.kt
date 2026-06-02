package com.ahn.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.domain.setting.usecase.ThemeUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val themeUseCases: ThemeUseCases,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themeUseCases.getThemeMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    fun saveThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            themeUseCases.saveThemeMode(themeMode)
        }
    }
}
