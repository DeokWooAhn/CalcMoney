package com.ahn.data.setting.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ahn.data.di.ThemePreferencesDataStore
import com.ahn.domain.setting.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferenceDataSource @Inject constructor(
    @param:ThemePreferencesDataStore private val dataStore: DataStore<Preferences>,
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    fun getThemeMode(): Flow<ThemeMode> {
        return dataStore.data
            .safePreferences()
            .map { prefs ->
                prefs[themeModeKey]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
            }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = themeMode.name
        }
    }

    private fun Flow<Preferences>.safePreferences() = catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
}
