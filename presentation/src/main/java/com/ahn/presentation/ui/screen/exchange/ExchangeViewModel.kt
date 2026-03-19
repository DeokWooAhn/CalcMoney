package com.ahn.presentation.ui.screen.exchange

import androidx.lifecycle.ViewModel
import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.usecase.GetExchangeRateUseCase
import com.ahn.domain.usecase.GetFavoriteCurrenciesUseCase
import com.ahn.domain.usecase.GetSupportedCurrenciesUseCase
import com.ahn.domain.usecase.ToggleFavoriteCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val getExchangeRateUseCase: GetExchangeRateUseCase,
    private val getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase,
    private val getFavoriteCurrenciesUseCase: GetFavoriteCurrenciesUseCase,
    private val toggleFavoriteCurrencyUseCase: ToggleFavoriteCurrencyUseCase,
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

    private fun handleUpdateFromAmount(amount: String) = intent {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            reduce {
                state.copy(
                    fromAmount = amount,
                    toAmount = calculateToAmount(amount, state.exchangeRate)
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

    private fun handleSelectToCurrency(currency: CurrencyInfo) = intent {
        if (currency == state.fromCurrency) {
            performSwapCurrencies()
        } else {
            reduce { state.copy(toCurrency = currency) }
            performFetchExchangeRate()
        }
    }

    private fun observeFavorites() = intent {
        getFavoriteCurrenciesUseCase().collect { codes ->
            reduce { state.copy(favoriteCurrencyCodes = codes.toSet()) }
        }
    }

    private fun handleToggleFavorite(currencyCode: String) = intent {
        toggleFavoriteCurrencyUseCase(currencyCode)
    }

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performLoadCurrencies() {
        try {
            reduce { state.copy(isLoading = true) }

            val currencies = getSupportedCurrenciesUseCase()
            val krw = currencies.find { it.code == "KRW" }
            val usd = currencies.find { it.code == "USD" }

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    fromCurrency = usd ?: currencies.firstOrNull() ?: state.fromCurrency,
                    toCurrency = krw ?: currencies.getOrNull(1) ?: currencies.firstOrNull()
                    ?: state.toCurrency,
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

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performFetchExchangeRate() {
        val fromCurrency = state.fromCurrency ?: return
        val toCurrency = state.toCurrency ?: return

        val requestedFrom = fromCurrency.code
        val requestedTo = toCurrency.code

        try {
            reduce { state.copy(isLoading = true) }

            // 여기서 일시 중단(suspend)되어 API 응답을 기다림 (동시성 꼬임 방지)
            val rate = getExchangeRateUseCase(
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
                    toAmount = calculateToAmount(state.fromAmount, rate)
                )
            }
        } catch (e: Exception) {
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar("환율 정보를 가져올 수 없습니다: ${e.message}")
            )
        }
    }

    private fun calculateToAmount(fromAmount: String, rate: Double): String {
        val amount = fromAmount.toDoubleOrNull() ?: 0.0
        val result = amount * rate

        return if (result > 0) String.format(Locale.US, "%.2f", result) else ""
    }
}