package com.ahn.data.currency.local.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.model.ExchangeCurrencySelection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.currencySelectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "currency_selection"
)

@Singleton
class CurrencySelectionDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val calculatorMainCurrencyCodeKey = stringPreferencesKey("calculator_main_currency_code")
    private val calculatorSubCurrencyCodeKey = stringPreferencesKey("calculator_sub_currency_code")
    private val exchangeFromCurrencyCodeKey = stringPreferencesKey("exchange_from_currency_code")
    private val exchangeToCurrencyCodeKey = stringPreferencesKey("exchange_to_currency_code")

    suspend fun getCalculatorSelection(): CalculatorCurrencySelection {
        return context.currencySelectionDataStore.data
            .safePreferences()
            .map { prefs ->
                CalculatorCurrencySelection(
                    mainCode = prefs[calculatorMainCurrencyCodeKey],
                    subCode = prefs[calculatorSubCurrencyCodeKey],
                )
            }
            .first()
    }

    suspend fun saveCalculatorMainCurrencyCode(code: String) {
        context.currencySelectionDataStore.edit { prefs ->
            prefs[calculatorMainCurrencyCodeKey] = code
        }
    }

    suspend fun saveCalculatorSubCurrencyCode(code: String) {
        context.currencySelectionDataStore.edit { prefs ->
            prefs[calculatorSubCurrencyCodeKey] = code
        }
    }

    suspend fun saveCalculatorSelection(mainCode: String, subCode: String) {
        context.currencySelectionDataStore.edit { prefs ->
            prefs[calculatorMainCurrencyCodeKey] = mainCode
            prefs[calculatorSubCurrencyCodeKey] = subCode
        }
    }

    suspend fun getExchangeSelection(): ExchangeCurrencySelection {
        return context.currencySelectionDataStore.data
            .safePreferences()
            .map { prefs ->
                ExchangeCurrencySelection(
                    fromCode = prefs[exchangeFromCurrencyCodeKey],
                    toCode = prefs[exchangeToCurrencyCodeKey],
                )
            }
            .first()
    }

    suspend fun saveExchangeFromCurrencyCode(code: String) {
        context.currencySelectionDataStore.edit { prefs ->
            prefs[exchangeFromCurrencyCodeKey] = code
        }
    }

    suspend fun saveExchangeToCurrencyCode(code: String) {
        context.currencySelectionDataStore.edit { prefs ->
            prefs[exchangeToCurrencyCodeKey] = code
        }
    }

    /**
     * Saves the exchange currency selection to persistent preferences.
     *
     * @param fromCode The currency code to set as the "from" currency.
     * @param toCode The currency code to set as the "to" currency.
     */
    suspend fun saveExchangeSelection(fromCode: String, toCode: String) {
        context.currencySelectionDataStore.edit { prefs ->
            prefs[exchangeFromCurrencyCodeKey] = fromCode
            prefs[exchangeToCurrencyCodeKey] = toCode
        }
    }

    /**
     * Recovers a preferences Flow from IO errors by emitting empty preferences when an [IOException] occurs.
     *
     * The resulting Flow rethrows non-IO exceptions.
     *
     * @receiver The source Flow of [Preferences].
     * @return A Flow that emits the original preferences values, or `emptyPreferences()` when an [IOException] is thrown by the source.
     */
    private fun Flow<Preferences>.safePreferences() = catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
}
