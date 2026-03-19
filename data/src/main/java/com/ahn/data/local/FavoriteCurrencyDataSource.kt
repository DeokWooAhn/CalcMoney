package com.ahn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "favorite_currencies"
)

@Singleton
class FavoriteCurrencyDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
){
    private val key = stringPreferencesKey("favorite_codes")

    fun getFavoriteCodes(): Flow<List<String>> {
        return context.dataStore.data.map { prefs ->
            val raw = prefs[key] ?: ""
            if (raw.isEmpty()) emptyList() else raw.split(",")
        }
    }

    suspend fun addFavorite(code: String) {
        context.dataStore.edit { prefs ->
            val current = getCurrent(prefs)
            if (code !in current) {
                prefs[key] = (current + code).joinToString(",")
            }
        }
    }

    suspend fun removeFavorite(code: String) {
        context.dataStore.edit { prefs ->
            val current = getCurrent(prefs)
            prefs[key] = current.filter { it != code }.joinToString(",")
        }
    }

    suspend fun isFavorite(code: String): Boolean {
        val current = context.dataStore.data.first()[key] ?: ""
        return code in current.split(",")
    }

    private fun getCurrent(prefs: Preferences): List<String> {
        val raw = prefs[key] ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(",")
    }
}