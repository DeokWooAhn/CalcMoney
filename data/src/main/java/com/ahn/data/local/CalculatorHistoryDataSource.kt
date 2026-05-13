package com.ahn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahn.domain.calculator.model.CalculatorHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.calculatorHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calculator_history"
)

@Singleton
class CalculatorHistoryDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("histories")
    private val itemSeparator = "\u001E"
    private val fieldSeparator = "\u001F"

    fun getHistories(): Flow<List<CalculatorHistory>> {
        return context.calculatorHistoryDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }.map { prefs ->
                decodeHistories(prefs[key].orEmpty())
            }
    }

    suspend fun addHistory(history: CalculatorHistory) {
        context.calculatorHistoryDataStore.edit { prefs ->
            val current = decodeHistories(prefs[key].orEmpty())
            prefs[key] = encodeHistories(current + history)
        }
    }

    suspend fun clearHistories() {
        context.calculatorHistoryDataStore.edit { prefs ->
            prefs[key] = ""
        }
    }

    private fun encodeHistories(histories: List<CalculatorHistory>): String {
        return histories.takeLast(20).joinToString(itemSeparator) { history ->
            "${history.expression}$fieldSeparator${history.result}"
        }
    }

    private fun decodeHistories(raw: String): List<CalculatorHistory> {
        if (raw.isEmpty()) return emptyList()

        return raw.split(itemSeparator).mapNotNull { item ->
            val parts = item.split(fieldSeparator, limit = 2)
            if (parts.size == 2) {
                CalculatorHistory(
                    expression = parts[0],
                    result = parts[1],
                )
            } else {
                null
            }
        }
    }
}
