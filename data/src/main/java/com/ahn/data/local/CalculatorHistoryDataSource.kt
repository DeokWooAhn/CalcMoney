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

    /**
     * Emits the persisted calculator history as a stream of decoded history entries.
     *
     * If the stored preference is missing or empty, the flow emits an empty list. If a read error
     * from DataStore is an `IOException`, the flow treats storage as empty and emits an empty list;
     * other exceptions are propagated.
     *
     * @return A Flow that emits the current `List<CalculatorHistory>` decoded from storage.
     * @throws Throwable Propagates non-`IOException` errors from the underlying DataStore.
     */
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

    /**
     * Appends a calculator history entry to the persisted histories in DataStore, keeping only the most recent 20 entries.
     *
     * @param history The history entry (expression and result) to append.
     */
    suspend fun addHistory(history: CalculatorHistory) {
        context.calculatorHistoryDataStore.edit { prefs ->
            val current = decodeHistories(prefs[key].orEmpty())
            prefs[key] = encodeHistories(current + history)
        }
    }

    /**
     * Clears all persisted calculator histories from the DataStore.
     *
     * This removes any stored history entries so subsequent reads will yield an empty list.
     */
    suspend fun clearHistories() {
        context.calculatorHistoryDataStore.edit { prefs ->
            prefs[key] = ""
        }
    }

    /**
     * Encode a list of calculator history items into a single string suitable for storing in Preferences.
     *
     * @param histories The histories to encode; only the last 20 entries are retained.
     * @return A single string where each history is encoded as `expression` followed by a field separator and `result`, and individual records are joined with the item separator.
    private fun encodeHistories(histories: List<CalculatorHistory>): String {
        return histories.takeLast(20).joinToString(itemSeparator) { history ->
            "${history.expression}$fieldSeparator${history.result}"
        }
    }

    /**
     * Parse an encoded histories string into a list of CalculatorHistory entries.
     *
     * @param raw Encoded histories string where individual records are separated by the item separator
     *            and each record's fields (expression and result) are separated by the field separator.
     * @return A list of parsed CalculatorHistory objects; returns an empty list if `raw` is empty
     *         or contains no well-formed records.
     */
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
