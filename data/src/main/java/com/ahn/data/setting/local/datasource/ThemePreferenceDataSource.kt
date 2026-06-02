package com.ahn.data.setting.local.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahn.domain.setting.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themePreferenceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_preferences",
)

@Singleton
class ThemePreferenceDataSource @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    fun getThemeMode(): Flow<ThemeMode> {
        return context.themePreferenceDataStore.data
            .safePreferences()
            .map { prefs ->
                prefs[themeModeKey]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM
            }
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        context.themePreferenceDataStore.edit { prefs ->
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
