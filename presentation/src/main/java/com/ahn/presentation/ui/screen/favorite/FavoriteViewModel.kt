package com.ahn.presentation.ui.screen.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.usecase.GetExchangeRateUseCase
import com.ahn.presentation.ui.screen.exchange.ExchangeContract
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

    fun onBaseAmountChanged(fromAmount: String) {
        currentBaseAmount = fromAmount
        rebuildItems()
    }

    private fun rebuildItems() {
        val base = currentBaseCurrency
        val byCode = currentCurrencies.associateBy { it.code }
        val baseAmount = currentBaseAmount.toDoubleOrNull() ?: 0.0

        if (base == null || currentFavoriteCodes.isEmpty() || currentCurrencies.isEmpty()) {
            _state.update { it.copy(isLoading = false, items = emptyList()) }
            return
        }

        val items = buildList {
            for (code in currentFavoriteCodes) {
                val info = byCode[code] ?: continue
                if (code == base.code) continue

                val rate = cachedRates[code] ?: continue
                val converted = baseAmount * rate

                add(
                    FavoriteContract.Item(
                        currency = info,
                        convertedAmount = String.format(Locale.US, "%.2f", converted),
                        rateLabel = "1 ${base.code} = ${String.format(Locale.US, "%.4f", rate)} ${info.code}"
                    )
                )
            }
        }

        _state.update { it.copy(isLoading = false, items = items) }
    }
}