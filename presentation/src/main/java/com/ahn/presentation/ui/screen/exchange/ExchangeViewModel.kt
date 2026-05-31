package com.ahn.presentation.ui.screen.exchange

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.currency.model.ExchangeCurrencySelection
import com.ahn.domain.currency.usecase.CurrencySelectionUseCases
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val exchangeUseCases: ExchangeUseCases,
    private val favoriteUseCases: FavoriteUseCases,
    private val currencySelectionUseCases: CurrencySelectionUseCases,
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
                    toAmount = exchangeUseCases.exchangeAmount(amount, state.exchangeRate)
                )
            }
        }
    }

    private fun handleSelectFromCurrency(currency: CurrencyInfo) = intent {
        if (currency == state.toCurrency) {
            performSwapCurrencies()
        } else {
            saveExchangeSelectionBestEffort {
                currencySelectionUseCases.saveExchangeFromCurrency(currency.code)
            }

            reduce { state.copy(fromCurrency = currency) }
            performFetchExchangeRate()
        }
    }

    private fun handleSelectToCurrency(currency: CurrencyInfo) = intent {
        if (currency == state.fromCurrency) {
            performSwapCurrencies()
        } else {
            saveExchangeSelectionBestEffort {
                currencySelectionUseCases.saveExchangeToCurrency(currency.code)
            }

            reduce { state.copy(toCurrency = currency) }
            performFetchExchangeRate()
        }
    }

    private fun observeFavorites() = intent {
        favoriteUseCases.getFavoriteCurrencies().collect { codes ->
            reduce { state.copy(favoriteCurrencyCodes = codes) }
        }
    }

    private fun handleToggleFavorite(currencyCode: String) = intent {
        val wasFavorite = currencyCode in state.favoriteCurrencyCodes

        runCatching {
            favoriteUseCases.toggleFavoriteCurrency(currencyCode)
        }.onSuccess {
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(
                        if (wasFavorite) R.string.favorite_removed else R.string.favorite_added
                    )
                )
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.favorite_change_failed)
                )
            )
        }
    }

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performLoadCurrencies() {
        try {
            reduce { state.copy(isLoading = true) }

            val currencies = exchangeUseCases.getSupportedCurrencies()
            val krw = currencies.find { it.code == "KRW" }
            val usd = currencies.find { it.code == "USD" }
            val savedSelection = getSavedExchangeSelectionOrNull()

            val preservedFrom = state.fromCurrency?.let { current ->
                currencies.find { it.code == current.code }
            }

            val fromCurrency = preservedFrom
                ?: currencies.find { it.code == savedSelection?.fromCode }
                ?: usd
                ?: currencies.firstOrNull()
                ?: state.fromCurrency

            val preservedTo = state.toCurrency?.let { current ->
                currencies.find { it.code == current.code }
            }

            val toCurrency = preservedTo
                ?: currencies.find { it.code == savedSelection?.toCode && it.code != fromCurrency?.code }
                ?: krw?.takeIf { it.code != fromCurrency?.code }
                ?: currencies.firstOrNull { it.code != fromCurrency?.code }
                ?: state.toCurrency

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    isLoading = false
                )
            }

            if (state.fromCurrency != null && state.toCurrency != null) {
                performFetchExchangeRate()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(
                        R.string.load_currency_list_failed,
                        listOf(e.message.orEmpty()),
                    )
                )
            )
        }
    }

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performSwapCurrencies() {
        val newFrom = state.toCurrency ?: return
        val newTo = state.fromCurrency ?: return
        val newFromAmount = state.toAmount.ifEmpty { "1" }

        reduce {
            state.copy(
                fromCurrency = newFrom,
                toCurrency = newTo,
                fromAmount = newFromAmount
            )
        }

        saveExchangeSelectionBestEffort {
            currencySelectionUseCases.saveExchangeSelection(
                fromCode = newFrom.code,
                toCode = newTo.code,
            )
        }

        performFetchExchangeRate()
    }

    private suspend fun getSavedExchangeSelectionOrNull(): ExchangeCurrencySelection? {
        return try {
            currencySelectionUseCases.getExchangeSelection()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ExchangeViewModel", "Failed to load saved exchange currency selection.", e)
            null
        }
    }

    private suspend fun saveExchangeSelectionBestEffort(action: suspend () -> Unit) {
        try {
            action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ExchangeViewModel", "Failed to save exchange currency selection.", e)
        }
    }

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
            val rateDate = exchangeUseCases.getLatestRateDate()

            // 응답이 돌아왔을 때 상태가 이미 바뀌었으면 적용하지 않음
            if (state.fromCurrency?.code != requestedFrom ||
                state.toCurrency?.code != requestedTo) return

            reduce {
                state.copy(
                    exchangeRate = rate,
                    exchangeRateDate = rateDate,
                    isLoading = false,
                    toAmount = exchangeUseCases.exchangeAmount(state.fromAmount, rate)
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(
                        R.string.load_exchange_rate_failed,
                        listOf(e.message.orEmpty()),
                    )
                )
            )
        }
    }
}
