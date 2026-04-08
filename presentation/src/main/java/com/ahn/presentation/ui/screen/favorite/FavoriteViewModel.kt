package com.ahn.presentation.ui.screen.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun onExchangeStateChanged(exchange: ExchangeContract.State) {
        loadJob?.cancel()
        val base = exchange.fromCurrency
        val codes = exchange.favoriteCurrencyCodes
        val all = exchange.availableCurrencies

        if (codes.isEmpty()) {
            _state.update { it.copy(isLoading = false, items = emptyList()) }
            return
        }

        if (base == null || all.isEmpty()) {
            _state.update { it.copy(isLoading = false, items = emptyList()) }
            return
        }

        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val byCode = all.associateBy { it.code }
            val items = mutableListOf<FavoriteContract.Item>()
            val baseAmountStr = exchange.fromAmount.ifEmpty { "0" }
            val baseAmount = baseAmountStr.toDoubleOrNull() ?: 0.0

            for (code in codes) {
                if (!isActive) return@launch
                val info = byCode[code] ?: continue
                if (code == base.code) continue

                runCatching {
                    val rate = getExchangeRateUseCase(base.code, code)
                    val converted = baseAmount * rate
                    val convertedStr = String.format(Locale.US, "%.2f", converted)
                    val rateStr = "1 ${base.code} = ${
                        String.format(Locale.US, "%.4f", rate)
                    } ${info.code}"
                    items += FavoriteContract.Item(info, convertedStr, rateStr)
                }
            }

            if (isActive) {
                _state.update { it.copy(isLoading = false, items = items) }
            }
        }
    }
}