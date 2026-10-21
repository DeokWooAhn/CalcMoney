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
import com.ahn.presentation.util.deviceCurrencyCode
import com.ahn.presentation.util.toExchangeRateErrorUiText
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
            is ExchangeContract.Intent.RefreshExchangeRates -> intent { performRefreshExchangeRates() }
        }
    }

    private fun handleUpdateFromAmount(amount: String) = intent {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            reduce {
                state.copy(
                    fromAmount = amount,
                    toAmount = exchangeUseCases.exchangeAmount(amount, state.exchangeRate),
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
                        if (wasFavorite) R.string.favorite_removed else R.string.favorite_added,
                    ),
                ),
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.favorite_change_failed),
                ),
            )
        }
    }

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performLoadCurrencies() {
        try {
            reduce { state.copy(isLoading = true) }

            val currencies = exchangeUseCases.getSupportedCurrencies()
            val savedSelection = getSavedExchangeSelectionOrNull()
            val selectedCurrencies = resolveExchangeCurrencies(
                currencies = currencies,
                savedSelection = savedSelection,
                currentFromCurrency = state.fromCurrency,
                currentToCurrency = state.toCurrency,
            )

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    fromCurrency = selectedCurrencies.from,
                    toCurrency = selectedCurrencies.to,
                    isLoading = false,
                )
            }

            if (selectedCurrencies.isComplete) {
                performFetchExchangeRate()
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    e.toExchangeRateErrorUiText(),
                ),
            )
        }
    }

    private fun resolveExchangeCurrencies(
        currencies: List<CurrencyInfo>,
        savedSelection: ExchangeCurrencySelection?,
        currentFromCurrency: CurrencyInfo?,
        currentToCurrency: CurrencyInfo?,
    ): SelectedExchangeCurrencies {
        // 화면에 있던 통화와 저장된 통화를 먼저 확정하고, 비어 있는 쪽만 기본값으로 채운다.
        // 출발·도착 통화는 따로 저장되므로 한쪽만 저장돼 있을 수 있는데, 그 선택을 기본값이 밀어내면 안 된다.
        val keptFrom = currencies.findByCode(currentFromCurrency?.code)
            ?: currencies.findByCode(savedSelection?.fromCode)
        val keptTo = currencies.findByCode(currentToCurrency?.code)
            ?: currencies.findByCode(savedSelection?.toCode)?.takeIf { it.code != keptFrom?.code }

        // 처음 열면 '미국 달러 → 기기 지역 통화'로 시작한다. 기기 통화가 미국 달러면 유로에서 바꾼다.
        val deviceCurrency = currencies.findByCode(deviceCurrencyCode()) ?: currencies.findByCode("KRW")
        val defaultFrom = currencies.findByCode(if (deviceCurrency?.code == "USD") "EUR" else "USD")

        val fromCurrency = keptFrom
            ?: (listOfNotNull(defaultFrom, deviceCurrency) + currencies).firstOrNull { it.code != keptTo?.code }
            ?: currentFromCurrency
        val toCurrency = keptTo
            ?: (listOfNotNull(deviceCurrency, defaultFrom) + currencies).firstOrNull { it.code != fromCurrency?.code }
            ?: currentToCurrency

        return SelectedExchangeCurrencies(
            from = fromCurrency,
            to = toCurrency,
        )
    }

    private fun List<CurrencyInfo>.findByCode(code: String?): CurrencyInfo? {
        return code?.let { targetCode ->
            find { it.code == targetCode }
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
                fromAmount = newFromAmount,
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
            val fetchedAt = exchangeUseCases.getLatestFetchedAt()

            // 응답이 돌아왔을 때 상태가 이미 바뀌었으면 적용하지 않음
            if (state.fromCurrency?.code != requestedFrom ||
                state.toCurrency?.code != requestedTo
            ) {
                    return
                }

            reduce {
                state.copy(
                    exchangeRate = rate,
                    exchangeRateDate = rateDate,
                    exchangeRateFetchedAt = fetchedAt,
                    isLoading = false,
                    toAmount = exchangeUseCases.exchangeAmount(state.fromAmount, rate),
                )
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    e.toExchangeRateErrorUiText(),
                ),
            )
        }
    }

    private suspend fun Syntax<ExchangeContract.State, ExchangeContract.SideEffect>.performRefreshExchangeRates() {
        try {
            reduce { state.copy(isLoading = true) }

            exchangeUseCases.refreshExchangeRates()

            val currencies = exchangeUseCases.getSupportedCurrencies()
            val selectedCurrencies = resolveExchangeCurrencies(
                currencies = currencies,
                savedSelection = getSavedExchangeSelectionOrNull(),
                currentFromCurrency = state.fromCurrency,
                currentToCurrency = state.toCurrency,
            )
            val rate = getExchangeRateOrDefault(
                fromCurrency = selectedCurrencies.from,
                toCurrency = selectedCurrencies.to,
            )
            val rateDate = exchangeUseCases.getLatestRateDate()
            val fetchedAt = exchangeUseCases.getLatestFetchedAt()

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    fromCurrency = selectedCurrencies.from,
                    toCurrency = selectedCurrencies.to,
                    exchangeRate = rate,
                    exchangeRateDate = rateDate,
                    exchangeRateFetchedAt = fetchedAt,
                    isLoading = false,
                    toAmount = exchangeUseCases.exchangeAmount(state.fromAmount, rate),
                )
            }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.exchange_rate_refresh_success),
                ),
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            reduce { state.copy(isLoading = false) }
            postSideEffect(
                ExchangeContract.SideEffect.ShowSnackBar(
                    e.toExchangeRateErrorUiText(),
                ),
            )
        }
    }

    private suspend fun getExchangeRateOrDefault(
        fromCurrency: CurrencyInfo?,
        toCurrency: CurrencyInfo?,
    ): Double {
        if (fromCurrency == null || toCurrency == null) return 0.0
        if (fromCurrency.code == toCurrency.code) return 1.0

        return exchangeUseCases.getExchangeRate(
            from = fromCurrency.code,
            to = toCurrency.code,
        )
    }
}

private data class SelectedExchangeCurrencies(
    val from: CurrencyInfo?,
    val to: CurrencyInfo?,
) {
    val isComplete: Boolean = from != null && to != null
}
