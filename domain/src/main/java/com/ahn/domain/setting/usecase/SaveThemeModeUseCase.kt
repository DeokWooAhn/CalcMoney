package com.ahn.domain.setting.usecase

import com.ahn.domain.setting.model.ThemeMode
import com.ahn.domain.setting.repository.ThemeRepository
import javax.inject.Inject

class SaveThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    suspend operator fun invoke(themeMode: ThemeMode) {
        repository.saveThemeMode(themeMode)
    }
}
