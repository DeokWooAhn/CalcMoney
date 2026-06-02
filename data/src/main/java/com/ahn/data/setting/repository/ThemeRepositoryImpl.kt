package com.ahn.data.setting.repository

import com.ahn.data.setting.local.datasource.ThemePreferenceDataSource
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.domain.setting.repository.ThemeRepository
import javax.inject.Inject

class ThemeRepositoryImpl @Inject constructor(private val dataSource: ThemePreferenceDataSource) : ThemeRepository {
    override fun getThemeMode() = dataSource.getThemeMode()

    override suspend fun saveThemeMode(themeMode: ThemeMode) {
        dataSource.saveThemeMode(themeMode)
    }
}
