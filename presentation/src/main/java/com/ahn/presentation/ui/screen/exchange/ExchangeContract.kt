package com.ahn.presentation.ui.screen.exchange

import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.util.UiText

interface ExchangeContract {
    data class State(
        val fromAmount: String = "1",
        val toAmount: String = "",
        val fromCurrency: CurrencyInfo? = null,
        val toCurrency: CurrencyInfo? = null,
        val availableCurrencies: List<CurrencyInfo> = emptyList(),
        val favoriteCurrencyCodes: List<String> = emptyList(),
        val exchangeRate: Double = 0.0,
        val exchangeRateDate: String = "",
        val exchangeRateFetchedAt: Long = 0L,
        val isLoading: Boolean = false,
        val failure: Failure? = null,
        val favoriteRates: FavoriteRates = FavoriteRates(),
    )

    data class FavoriteRateItem(
        val currency: CurrencyInfo,
        val convertedAmount: String,
        val rateLabel: String,
    )

    data class FavoriteRates(
        val isLoading: Boolean = false,
        val items: List<FavoriteRateItem> = emptyList(),
        /**
         * 즐겨찾기가 현재 기준 통화 하나뿐일 때의 기준 통화 코드.
         * 기준 통화는 카드로 만들지 않으므로 목록이 비는데, 이를 로드 실패와 구분하는 데 쓴다.
         */
        val baseOnlyFavoriteCode: String? = null,
    )

    data class Failure(
        val scope: Scope,
        val message: UiText,
    ) {
        enum class Scope { CURRENCIES, RATE }
    }

    sealed interface Intent {
        data class UpdateFromAmount(val amount: String) : Intent

        data class SelectFromCurrency(val currency: CurrencyInfo) : Intent

        data class SelectToCurrency(val currency: CurrencyInfo) : Intent

        data class ToggleFavorite(val currencyCode: String) : Intent

        object SwapCurrencies : Intent

        object LoadCurrencies : Intent

        object RefreshExchangeRates : Intent
    }

    sealed interface SideEffect {
        data class ShowSnackBar(val message: UiText) : SideEffect
    }
}
