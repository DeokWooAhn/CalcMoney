package com.ahn.domain.setting.repository

import com.ahn.domain.setting.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    fun getThemeMode(): Flow<ThemeMode>

    suspend fun saveThemeMode(themeMode: ThemeMode)
}
