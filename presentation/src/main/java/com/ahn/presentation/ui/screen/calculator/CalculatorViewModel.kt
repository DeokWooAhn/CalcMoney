package com.ahn.presentation.ui.screen.calculator

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.usecase.CalculatorUseCases
import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.currency.usecase.CurrencySelectionUseCases
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText
import com.ahn.presentation.util.toExchangeRateErrorUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorUseCases: CalculatorUseCases,
    private val exchangeUseCases: ExchangeUseCases,
    private val favoriteUseCases: FavoriteUseCases,
    private val currencySelectionUseCases: CurrencySelectionUseCases,
) : ViewModel(), ContainerHost<CalculatorContract.State, CalculatorContract.SideEffect> {
    override val container = container(
        initialState = CalculatorContract.State(),
    ) {
        performLoadCurrencies()
        observeSavedData()
    }

    companion object {
        private const val MAX_NUMBER_LENGTH = 15
        private const val MAX_HISTORY_COUNT = 20
    }

    private val expressionReducer = CalculatorExpressionReducer(
        calculateExpression = calculatorUseCases.calculateExpression,
        convertExchangeAmount = exchangeUseCases.convertExchangeAmount,
    )

    fun updateCursorPosition(newCursorPosition: Int) = intent {
        reduce {
            state.copy(cursorPosition = newCursorPosition.coerceIn(0, state.expression.length))
        }
    }

    fun processIntent(intent: CalculatorContract.Intent) {
        when (intent) {
            is CalculatorContract.Intent.Input -> when (intent.token) {
                is CalculatorToken.Number -> handleNumberInput(intent.token.value)
                is CalculatorToken.Operator -> handleOperatorInput(intent.token.value)
                is CalculatorToken.Dot -> handleDotInput()
                is CalculatorToken.Parenthesis -> handleParenthesisInput()
            }

            is CalculatorContract.Intent.SelectExchangeCurrency -> handleSelectExchangeCurrency(intent.currency)
            is CalculatorContract.Intent.SelectMainExchangeCurrency -> handleSelectMainExchangeCurrency(intent.currency)

            is CalculatorContract.Intent.ToggleFavorite -> handleToggleFavorite(intent.currencyCode)

            is CalculatorContract.Intent.Delete -> handleDelete()
            is CalculatorContract.Intent.Clear -> handleClear()
            is CalculatorContract.Intent.Calculate -> handleCalculate()

            CalculatorContract.Intent.SwapExchangeCurrencies -> intent { performSwapExchangeCurrencies() }
            CalculatorContract.Intent.ClearHistory -> handleClearHistory()
        }
    }

    private fun handleToggleFavorite(currencyCode: String) = intent {
        val wasFavorite = currencyCode in state.favoriteCurrencyCodes

        runCatching {
            favoriteUseCases.toggleFavoriteCurrency(currencyCode)
        }.onSuccess {
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(
                        if (wasFavorite) R.string.favorite_removed else R.string.favorite_added,
                    ),
                ),
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.favorite_change_failed),
                ),
            )
        }
    }

    private fun handleNumberInput(number: String) = intent {
        when (val result = expressionReducer.inputNumber(state, number)) {
            CalculatorExpressionResult.MaxNumberLengthExceeded -> {
                postSideEffect(
                    CalculatorContract.SideEffect.ShowSnackBar(
                        UiText.StringResource(
                            R.string.max_number_length_error,
                            listOf(MAX_NUMBER_LENGTH),
                        ),
                    ),
                )
            }

            is CalculatorExpressionResult.Updated -> reduce { result.state }
        }
    }

    private fun handleOperatorInput(operator: String) = intent {
        expressionReducer.inputOperator(state, operator)?.let { nextState ->
            reduce { nextState }
        }
    }

    private fun handleDotInput() = intent {
        expressionReducer.inputDot(state)?.let { nextState ->
            reduce { nextState }
        }
    }

    private fun handleParenthesisInput() = intent {
        reduce { expressionReducer.inputParenthesis(state) }
    }

    private fun handleDelete() = intent {
        expressionReducer.delete(state)?.let { nextState ->
            reduce { nextState }
        }
    }

    private fun handleClear() = intent {
        reduce {
            state.copy(
                expression = "",
                cursorPosition = 0,
                previewResult = "",
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
                repeatOperation = null,
                isCalculatedResult = false,
                isError = false,
                errorMessage = null,
            )
        }
    }

    /**
     * 현재 표현식을 계산하고(필요한 경우 반복 연산을 적용), 화면 상태를 업데이트합니다.
     *
     * 표현식이 비어 있으면 이 intent는 아무 작업도 하지 않습니다.
     * 숫자 표현식과 반복 연산이 모두 존재하는 경우, 계산 전에 반복 연산을 표현식 뒤에 추가합니다.
     * 계산 중 오류가 발생하면 상태를 오류로 표시하고 스낵바 사이드 이펙트를 발생시킵니다.
     * 계산에 성공하면 표현식은 계산된 결과로 대체되고,
     * 미리보기/변환된 미리보기 값은 초기화되며,
     * 반복 연산이 업데이트되고,
     * 결과는 계산 완료된 결과로 표시되며,
     * 해당 계산 내역이 히스토리에 추가됩니다.
     */
    private fun handleCalculate() = intent {
        val expression = state.expression
        if (expression.isEmpty()) return@intent

        val shouldRepeatOperation = state.repeatOperation != null && expression.toDoubleOrNull() != null
        if (!shouldRepeatOperation && !expressionReducer.hasBinaryOperator(expression)) return@intent

        val expressionToCalculate =
            if (shouldRepeatOperation) {
                expression + state.repeatOperation
            } else {
                expression
            }

        val result = calculatorUseCases.calculateExpression.calculate(expressionToCalculate)

        if (result == "Error") {
            reduce {
                state.copy(
                    isError = true,
                    errorMessage = UiText.StringResource(R.string.calculator_error),
                )
            }

            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.invalid_expression),
                ),
            )
            return@intent
        }

        val nextRepeatOperation = calculatorUseCases.extractRepeatOperation(expressionToCalculate)
            ?: state.repeatOperation

        val historyItem = CalculatorContract.HistoryItem(
            expression = expressionToCalculate,
            result = result,
        )

        val nextHistories = (state.histories + historyItem).takeLast(MAX_HISTORY_COUNT)

        val isHistorySaved = runCatching {
            calculatorUseCases.addHistory(
                CalculatorHistory(
                    expression = historyItem.expression,
                    result = historyItem.result,
                ),
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Log.e("CalculatorViewModel", "Failed to save calculator history.", e)
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.calculator_history_save_failed),
                ),
            )
        }.isSuccess

        reduce {
            expressionReducer.buildNewExpressionState(
                currentState = state,
                newExpression = result,
                newCursorPos = result.length,
            ).copy(
                expression = result,
                cursorPosition = result.length,
                previewResult = "",
                convertedPreviewAmount = "",
                repeatOperation = nextRepeatOperation,
                isCalculatedResult = true,
                histories = if (isHistorySaved) nextHistories else state.histories,
            )
        }
    }

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performLoadCurrencies() {
        try {
            val currencies = exchangeUseCases.getSupportedCurrencies()
            val deviceCurrencyCode = getDeviceCurrencyCode()
            val savedSelection = getSavedCalculatorSelectionOrNull()

            val mainCurrency = resolveCalculatorMainCurrency(
                currencies = currencies,
                currentCurrency = state.mainExchangeCurrency,
                savedSelection = savedSelection,
                deviceCurrencyCode = deviceCurrencyCode,
            )
            val subCurrency = resolveCalculatorSubCurrency(
                currencies = currencies,
                currentCurrency = state.selectedExchangeCurrency,
                savedSelection = savedSelection,
                mainCurrency = mainCurrency,
            )

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    mainExchangeCurrency = mainCurrency,
                    selectedExchangeCurrency = subCurrency,
                )
            }

            performFetchExchangeRate()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    e.toExchangeRateErrorUiText(),
                ),
            )

            Log.e(
                "CalculatorViewModel",
                "Failed to load currency list.",
                e,
            )
        }
    }

    private suspend fun getSavedCalculatorSelectionOrNull(): CalculatorCurrencySelection? {
        return try {
            currencySelectionUseCases.getCalculatorSelection()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CalculatorViewModel", "Failed to load saved calculator currency selection.", e)
            null
        }
    }

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performFetchExchangeRate() {
        val from = state.mainExchangeCurrency ?: return
        val to = state.selectedExchangeCurrency ?: return

        try {
            val rate = if (from.code == to.code) {
                1.0
            } else {
                exchangeUseCases.getExchangeRate(from = from.code, to = to.code)
            }

            reduce {
                expressionReducer.run {
                    state.copy(exchangeRate = rate).withConvertedAmounts()
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    e.toExchangeRateErrorUiText(),
                ),
            )
        }
    }

    private suspend fun Syntax<
        CalculatorContract.State,
        CalculatorContract.SideEffect,
    >.performSwapExchangeCurrencies() {
        val from = state.mainExchangeCurrency ?: return
        val to = state.selectedExchangeCurrency ?: return

        reduce {
            state.copy(
                mainExchangeCurrency = to,
                selectedExchangeCurrency = from,
                exchangeRate = 0.0,
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
            )
        }

        saveCalculatorSelectionBestEffort {
            currencySelectionUseCases.saveCalculatorSelection(
                mainCode = to.code,
                subCode = from.code,
            )
        }

        performFetchExchangeRate()
    }

    private fun handleSelectMainExchangeCurrency(currency: CurrencyInfo) = intent {
        if (currency.code == state.mainExchangeCurrency?.code) return@intent

        saveCalculatorSelectionBestEffort {
            currencySelectionUseCases.saveCalculatorMainCurrency(currency.code)
        }

        reduce {
            state.copy(
                mainExchangeCurrency = currency,
                exchangeRate = 0.0,
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
            )
        }

        performFetchExchangeRate()
    }

    private fun handleSelectExchangeCurrency(currency: CurrencyInfo) = intent {
        if (currency.code == state.selectedExchangeCurrency?.code) return@intent

        saveCalculatorSelectionBestEffort {
            currencySelectionUseCases.saveCalculatorSubCurrency(currency.code)
        }

        reduce {
            state.copy(
                selectedExchangeCurrency = currency,
                exchangeRate = 0.0,
                convertedExpressionAmount = "",
                convertedPreviewAmount = "",
            )
        }

        performFetchExchangeRate()
    }

    private suspend fun saveCalculatorSelectionBestEffort(action: suspend () -> Unit) {
        try {
            action()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CalculatorViewModel", "Failed to save calculator currency selection.", e)
        }
    }

    private fun handleClearHistory() = intent {
        runCatching {
            calculatorUseCases.clearHistory()
        }.onSuccess {
            reduce { state.copy(histories = emptyList()) }
        }.onFailure { e ->
            Log.e("CalculatorViewModel", "Failed to clear calculator history.", e)
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.calculator_history_delete_failed),
                ),
            )
        }
    }

    private fun observeSavedData() = intent {
        combine(
            calculatorUseCases.getHistory(),
            favoriteUseCases.getFavoriteCurrencies(),
        ) { histories, favoriteCodes ->
            histories to favoriteCodes
        }.collect { (histories, favoriteCodes) ->
            reduce {
                state.copy(
                    histories = histories.map {
                        CalculatorContract.HistoryItem(
                            expression = it.expression,
                            result = it.result,
                        )
                    },
                    favoriteCurrencyCodes = favoriteCodes,
                )
            }
        }
    }
}

private fun getDeviceCurrencyCode(): String {
    return runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("KRW")
}

private fun resolveCalculatorMainCurrency(
    currencies: List<CurrencyInfo>,
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    deviceCurrencyCode: String,
): CurrencyInfo? {
    return currencies.find { it.code == currentCurrency?.code }
        ?: currencies.find { it.code == savedSelection?.mainCode }
        ?: currencies.find { it.code == deviceCurrencyCode }
        ?: currencies.find { it.code == "KRW" }
        ?: currencies.firstOrNull()
}

private fun resolveCalculatorSubCurrency(
    currencies: List<CurrencyInfo>,
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    mainCurrency: CurrencyInfo?,
): CurrencyInfo? {
    val mainCurrencyCode = mainCurrency?.code

    return currencies.find { it.code == currentCurrency?.code && it.code != mainCurrencyCode }
        ?: currencies.find { it.code == savedSelection?.subCode && it.code != mainCurrencyCode }
        ?: currencies.find { it.code == "USD" && it.code != mainCurrencyCode }
        ?: currencies.firstOrNull { it.code != mainCurrencyCode }
}
