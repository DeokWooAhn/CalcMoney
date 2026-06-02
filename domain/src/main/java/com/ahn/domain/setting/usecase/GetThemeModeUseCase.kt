package com.ahn.domain.setting.usecase

import com.ahn.domain.setting.model.ThemeMode
import com.ahn.domain.setting.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(private val repository: ThemeRepository) {
    operator fun invoke(): Flow<ThemeMode> {
        return repository.getThemeMode()
    }
}
