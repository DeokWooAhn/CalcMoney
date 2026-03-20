package com.ahn.presentation.ui.screen.exchange

import com.ahn.domain.model.CurrencyInfo

interface ExchangeContract {
    data class State(
        val fromAmount: String = "1",
        val toAmount: String = "",
        val fromCurrency: CurrencyInfo? = null,
        val toCurrency: CurrencyInfo? = null,
        val availableCurrencies: List<CurrencyInfo> = emptyList(),
        val favoriteCurrencyCodes: List<String> = emptyList(),
        val exchangeRate: Double = 0.0,
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class UpdateFromAmount(val amount: String) : Intent
        data class SelectFromCurrency(val currency: CurrencyInfo) : Intent
        data class SelectToCurrency(val currency: CurrencyInfo) : Intent
        data class ToggleFavorite(val currencyCode: String) : Intent
        object SwapCurrencies : Intent
        object LoadCurrencies : Intent
    }

    sealed interface SideEffect {
        data class ShowSnackBar(val message: String) : SideEffect
    }
}