package com.ahn.presentation.ui.screen.exchange

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahn.domain.usecase.GetExchangeRateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val getExchangeRateUseCase: GetExchangeRateUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ExchangeContract.State())
    val state: StateFlow<ExchangeContract.State> = _state.asStateFlow()

    private val _sideEffect = MutableSharedFlow<ExchangeContract.SideEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val sideEffect: SharedFlow<ExchangeContract.SideEffect> = _sideEffect.asSharedFlow()

    init {
        fetchExchangeRate()
    }

    fun processIntent(intent: ExchangeContract.Intent) {
        when (intent) {
            is ExchangeContract.Intent.UpdateFromAmount -> {
                updateFromAmount(intent.amount)
            }

            is ExchangeContract.Intent.SelectFromCurrency -> {
                selectFromCurrency(intent.currency)
            }

            is ExchangeContract.Intent.SelectToCurrency -> {
                selectToCurrency(intent.currency)
            }

            is ExchangeContract.Intent.SwapCurrencies -> {
                swapCurrencies()
            }
        }
    }

    private fun updateFromAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _state.update { it.copy(fromAmount = amount) }
            calculateExchange()
        }
    }

    private fun selectFromCurrency(currency: Currency) {
        if (currency == _state.value.toCurrency) {
            swapCurrencies()
        } else {
            _state.update { it.copy(fromCurrency = currency) }
            fetchExchangeRate()
        }
    }

    private fun selectToCurrency(currency: Currency) {
        if (currency == _state.value.fromCurrency) {
            swapCurrencies()
        } else {
            _state.update { it.copy(toCurrency = currency) }
            fetchExchangeRate()
        }
    }

    private fun swapCurrencies() {
        _state.update {
            it.copy(
                fromCurrency = it.toCurrency,
                toCurrency = it.fromCurrency,
                fromAmount = it.toAmount.ifEmpty { "1" })
        }
        fetchExchangeRate()
    }

    private fun fetchExchangeRate() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val rate = getExchangeRateUseCase(
                    from = _state.value.fromCurrency.code,
                    to = _state.value.toCurrency.code,
                )

                _state.update {
                    it.copy(exchangeRate = rate, isLoading = false)
                }
                calculateExchange()
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                sendSideEffect(ExchangeContract.SideEffect.ShowSnackBar(
                    "환율 정보를 가져올 수 없습니다: ${e.message}"))
            }
        }
    }

    private fun calculateExchange() {
        val currentState = _state.value
        val amount = currentState.fromAmount.toDoubleOrNull() ?: 0.0
        val result = amount * currentState.exchangeRate

        _state.update {
            it.copy(toAmount = if (result > 0) String.format(Locale.US, "%.2f", result) else "")
        }
    }

    private fun sendSideEffect(sideEffect: ExchangeContract.SideEffect) {
        viewModelScope.launch {
            _sideEffect.tryEmit(sideEffect)
        }
    }

    // mock 환율 데이터
//    private fun getMockExchangeRate(from: Currency, to: Currency): Double {
//        return when {
//            from == to -> 1.0
//            from == Currency.KRW && to == Currency.USD -> 0.00075
//            from == Currency.USD && to == Currency.KRW -> 1533.33
//            from == Currency.KRW && to == Currency.JPY -> 0.11
//            from == Currency.JPY && to == Currency.KRW -> 9.09
//            from == Currency.USD && to == Currency.JPY -> 149.50
//            from == Currency.JPY && to == Currency.USD -> 0.0067
//            else -> 1.0
//        }
//    }
}