package com.ahn.presentation.ui.screen.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.usecase.BuildFavoriteRatesUseCase
import com.ahn.domain.usecase.GetExchangeRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val getExchangeRateUseCase: GetExchangeRateUseCase,
    private val buildFavoriteRatesUseCase: BuildFavoriteRatesUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FavoriteContract.State())
    val state = _state.asStateFlow()

    private var loadJob: Job? = null

    private var currentBaseCurrency: CurrencyInfo? = null
    private var currentBaseAmount: String = "1"
    private var currentCurrencies: List<CurrencyInfo> = emptyList()
    private var currentFavoriteCodes: List<String> = emptyList()

    private var cachedRates: Map<String, Double> = emptyMap()

    fun onExchangeStateChanged(
        fromCurrency: CurrencyInfo?,
        favoriteCurrencyCodes: List<String>,
        availableCurrencies: List<CurrencyInfo>,
    ) {
        currentBaseCurrency = fromCurrency
        currentFavoriteCodes = favoriteCurrencyCodes
        currentCurrencies = availableCurrencies

        loadJob?.cancel()

        val base = fromCurrency
        if (base == null || favoriteCurrencyCodes.isEmpty() || availableCurrencies.isEmpty()) {
            cachedRates = emptyMap()
            _state.update { it.copy(isLoading = false, items = emptyList()) }
            return
        }

        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val byCode = availableCurrencies.associateBy { it.code }
            val nextRates = mutableMapOf<String, Double>()

            for (code in favoriteCurrencyCodes) {
                if (!isActive) return@launch
                if (code == base.code) continue
                if (byCode[code] == null) continue

                runCatching {
                    getExchangeRateUseCase(base.code, code)
                }.onSuccess { rate ->
                    nextRates[code] = rate
                }
            }

            if (!isActive) return@launch

            cachedRates = nextRates
            rebuildItems()
        }
    }

    /**
     * Update the current base amount and refresh the favorite rates display.
     *
     * @param fromAmount The new base amount as entered by the user (string form, e.g. "1", "12.34").
     */
    fun onBaseAmountChanged(fromAmount: String) {
        currentBaseAmount = fromAmount
        rebuildItems()
    }

    /**
     * Builds UI items for favorite currency conversions and updates the view state.
     *
     * Requests conversion data from the injected use case using the current base currency,
     * base amount, favorite currency codes, available currencies, and cached rates,
     * formats each converted amount to two decimal places (Locale.US) and each rate label as
     * "1 {baseCurrencyCode} = {rate (4 decimals)} {currency.code}", then sets the state's
     * `isLoading` to `false` and `items` to the resulting list.
     */
    private fun rebuildItems() {
        val favoriteRates = buildFavoriteRatesUseCase(
            baseCurrency = currentBaseCurrency,
            baseAmount = currentBaseAmount,
            favoriteCurrencyCodes = currentFavoriteCodes,
            availableCurrencies = currentCurrencies,
            ratesByCode = cachedRates,
        )

        val items = favoriteRates.map { rateInfo ->
            FavoriteContract.Item(
                currency = rateInfo.currency,
                convertedAmount = String.format(Locale.US, "%.2f", rateInfo.convertedAmount),
                rateLabel = "1 ${rateInfo.baseCurrencyCode} = ${
                    String.format(Locale.US, "%.4f", rateInfo.rate)
                } ${rateInfo.currency.code}",
            )
        }

        _state.update {
            it.copy(
                isLoading = false,
                items = items
            )
        }
    }
}