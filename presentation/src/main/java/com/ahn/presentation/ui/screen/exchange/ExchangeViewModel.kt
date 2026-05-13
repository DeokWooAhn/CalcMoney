package com.ahn.presentation.ui.screen.exchange

import androidx.lifecycle.ViewModel
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val exchangeUseCases: ExchangeUseCases,
    private val favoriteUseCases: FavoriteUseCases,
) : ViewModel(), ContainerHost<ExchangeContract.State, ExchangeContract.SideEffect> {

    override val container = container(
        initialState = ExchangeContract.State(),
    ) {
        performLoadCurrencies()
        observeFavorites()
    }

    fun processIntent(intent: ExchangeContract.Intent) {
        when (intent) {
            is ExchangeContract.Intent.UpdateFromAmount -> handleUpdateFromAmount(intent.amount)
            is ExchangeContract.Intent.SelectFromCurrency -> handleSelectFromCurrency(intent.currency)
            is ExchangeContract.Intent.SelectToCurrency -> handleSelectToCurrency(intent.currency)
            is ExchangeContract.Intent.ToggleFavorite -> handleToggleFavorite(intent.currencyCode)
            is ExchangeContract.Intent.SwapCurrencies -> intent { performSwapCurrencies() }
            is ExchangeContract.Intent.LoadCurrencies -> intent { performLoadCurrencies() }
        }
    }

    /**
     * Updates the state's `fromAmount` and computes the matching `toAmount` when the input is empty or a valid numeric string.
     *
     * @param amount The entered amount as a string; accepted values are an empty string or digits with an optional decimal point (e.g., "123", "12.34", ""). 
     */
    private fun handleUpdateFromAmount(amount: String) = intent {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            reduce {
                state.copy(
                    fromAmount = amount,
                    toAmount = exchangeUseCases.exchangeAmount(amount, state.exchangeRate)
                )
            }
        }
    }

    private fun handleSelectFromCurrency(currency: CurrencyInfo) = intent {
        if (currency == state.toCurrency) {
            performSwapCurrencies()
        } else {
            reduce { state.copy(fromCurrency = currency) }
            performFetchExchangeRate()
        }
    }

    /**
     * Selects the destination currency for conversion.
     *
     * If the chosen currency equals the currently selected source currency, the source and
     * destination currencies are swapped. Otherwise the destination currency is set to the
     * chosen value and a new exchange rate is fetched.
     *
     * @param currency The currency to select as the destination (to) currency.
     */
    private fun handleSelectToCurrency(currency: CurrencyInfo) = intent {
        if (currency == state.fromCurrency) {
            performSwapCurrencies()
        } else {
            reduce { state.copy(toCurrency = currency) }
            performFetchExchangeRate()
        }
    }

    /**
     * Observes the favorite currency codes stream and updates the state with the latest codes.
     *
     * When the observed list emits, `favoriteCurrencyCodes` in the view model state is replaced with the emitted list.
     */
    private fun observeFavorites() = intent {
        favoriteUseCases.getFavoriteCurrencies().collect { codes ->
            reduce { state.copy(favoriteCurrencyCodes = codes) }
        }
    }

    /**
     * Toggles whether the specified currency is a favorite and emits a confirmation snackbar.
     *
     * If the currency was a favorite, it will be removed and a "removed" snackbar is shown;
     * otherwise it will be added and an "added" snackbar is shown.
     *
     * @param currencyCode The currency code to toggle (e.g., "USD", "KRW").
     */
    private fun handleToggleFavorite(currencyCode: String) = intent {
        val wasFavorite = currencyCode in state.favoriteCurrencyCodes

        favoriteUseCases.toggleFavoriteCurrency(currencyCode)

        postSideEffect(
            ExchangeContract.SideEffect.ShowSnackBar(
                if (wasFavorite) "즐겨찾기가 해제되었습니다."
                else "즐겨찾기에 추가되었습니다."
            )
        )
    }

    /**
     * Loads supported currencies, updates the state's available and selected currencies, and triggers an exchange-rate fetch when both selections are present.
     *
     * Updates state to indicate loading, replaces the available currency list, preserves prior selections when possible, and chooses sensible defaults (prefer USD for from, KRW for to) when no preserved selection exists. Clears the loading flag when finished. If both from and to currencies are set after the update, initiates fetching of the exchange rate. On failure, clears the loading flag and emits a snack-bar side effect with an error message.
     */
    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performLoadCurrencies() {
        try {
            reduce { state.copy(isLoading = true) }

            val currencies = exchangeUseCases.getSupportedCurrencies()
            val krw = currencies.find { it.code == "KRW" }
            val usd = currencies.find { it.code == "USD" }

            val preservedFrom = state.fromCurrency?.let { current ->
                currencies.find { it.code == current.code }
            }

            val preservedTo = state.toCurrency?.let { current ->
                currencies.find { it.code == current.code }
            }

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    fromCurrency = preservedFrom ?: usd ?: currencies.firstOrNull() ?: state.fromCurrency,
                    toCurrency = preservedTo ?: krw ?: currencies.getOrNull(1) ?: currencies.firstOrNull() ?: state.toCurrency,
                    isLoading = false
                )
            }

            if (state.fromCurrency != null && state.toCurrency != null) {
                performFetchExchangeRate()
            }
        } catch (e: Exception) {
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar("통화 목록을 불러올 수 없습니다: ${e.message}")
            )
        }
    }

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performSwapCurrencies() {
        reduce {
            val newFromAmount = state.toAmount.ifEmpty { "1" }
            state.copy(
                fromCurrency = state.toCurrency,
                toCurrency = state.fromCurrency,
                fromAmount = newFromAmount
            )
        }
        performFetchExchangeRate()
    }

    /**
     * Fetches the latest exchange rate for the currently selected from/to currencies and updates the state.
     *
     * Sets `isLoading` while fetching; if the selected currencies change before the response arrives, the result is discarded.
     * On success updates `exchangeRate`, recalculates `toAmount`, and clears `isLoading`. On failure clears `isLoading` and emits a `ShowSnackBar` side effect with an error message.
     */
    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performFetchExchangeRate() {
        val fromCurrency = state.fromCurrency ?: return
        val toCurrency = state.toCurrency ?: return

        val requestedFrom = fromCurrency.code
        val requestedTo = toCurrency.code

        try {
            reduce { state.copy(isLoading = true) }

            // 여기서 일시 중단(suspend)되어 API 응답을 기다림 (동시성 꼬임 방지)
            val rate = exchangeUseCases.getExchangeRate(
                from = requestedFrom,
                to = requestedTo,
            )

            // 응답이 돌아왔을 때 상태가 이미 바뀌었으면 적용하지 않음
            if (state.fromCurrency?.code != requestedFrom ||
                state.toCurrency?.code != requestedTo) return

            reduce {
                state.copy(
                    exchangeRate = rate,
                    isLoading = false,
                    toAmount = exchangeUseCases.exchangeAmount(state.fromAmount, rate)
                )
            }
        } catch (e: Exception) {
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar("환율 정보를 가져올 수 없습니다: ${e.message}")
            )
        }
    }
}
