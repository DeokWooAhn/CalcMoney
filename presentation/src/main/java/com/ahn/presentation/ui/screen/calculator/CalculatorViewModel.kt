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
        initialState = CalculatorContract.State()
    ) {
        performLoadCurrencies()
        observeSavedData()
    }

    companion object {
        private const val MAX_NUMBER_LENGTH = 15
        private const val MAX_HISTORY_COUNT = 20
        private val OPERATORS = listOf("+", "-", "×", "÷")
    }

    fun updateCursorPosition(newCursorPosition: Int) = intent {
        reduce {
            state.copy(cursorPosition = newCursorPosition.coerceIn(0, state.expression.length))
        }
    }

    fun processIntent(intent: CalculatorContract.Intent) {
        when (intent) {
            is CalculatorContract.Intent.Input -> handleInput(intent.token)
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
                        if (wasFavorite) R.string.favorite_removed else R.string.favorite_added
                    )
                )
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.favorite_change_failed)
                )
            )
        }
    }

    private fun handleInput(token: CalculatorToken) {
        when (token) {
            is CalculatorToken.Number -> handleNumberInput(token.value)
            is CalculatorToken.Operator -> handleOperatorInput(token.value)
            is CalculatorToken.Dot -> handleDotInput()
            is CalculatorToken.Parenthesis -> handleParenthesisInput()
        }
    }

    private fun handleNumberInput(number: String) = intent {
        val expression = state.expression
        val cursorPosition = state.cursorPosition

        if (!canInsertNumber(expression, cursorPosition)) {
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(
                        R.string.max_number_length_error,
                        listOf(MAX_NUMBER_LENGTH),
                    )
                )
            )
            return@intent
        }

        if (expression == "0" && number != "0") {
            reduce { buildNewExpressionState(state, number, number.length) }
            return@intent
        }

        reduce { buildInsertState(state, number) }
    }

    private fun handleOperatorInput(operator: String) = intent {
        val expression = state.expression
        val cursorPosition = state.cursorPosition

        if (expression.isEmpty() || cursorPosition == 0) return@intent

        val charBefore = expression.getOrNull(cursorPosition - 1)

        // 이전 문자가 연산자면 교체
        if (charBefore != null && charBefore.toString() in OPERATORS) {
            val newExpression = StringBuilder(expression)
                .deleteCharAt(cursorPosition - 1)
                .insert(cursorPosition - 1, operator)
                .toString()

            reduce { buildNewExpressionState(state, newExpression, cursorPosition) }
            return@intent
        }

        if (charBefore == '(') return@intent

        reduce { buildInsertState(state, operator) }
    }

    private fun handleDotInput() = intent {
        val expression = state.expression
        val cursorPosition = state.cursorPosition

        if (expression.isEmpty()) {
            reduce { buildNewExpressionState(state, "0.", 2) }
            return@intent
        }

        val currentNumberBlock = getCurrentNumberBlock(expression, cursorPosition)
        if (currentNumberBlock.contains('.')) return@intent

        val charBefore = expression.getOrNull(cursorPosition - 1)
        if (charBefore != null && (charBefore.toString() in OPERATORS || charBefore == '(')) {
            reduce { buildInsertState(state, "0.") }
            return@intent
        }

        reduce { buildInsertState(state, ".") }
    }

    private fun handleParenthesisInput() = intent {
        val expression = state.expression
        val cursorPosition = state.cursorPosition
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val charBefore = if (cursorPosition > 0) expression.getOrNull(cursorPosition - 1) else null

        val textToInsert = when {
            // 닫는 괄호가 필요한 상황
            openCount > closeCount && charBefore != null &&
                    (charBefore.isDigit() || charBefore == ')') -> ")"
            // 숫자나 닫는 괄호 뒤에 여는 괄호면 곱하기 추가
            charBefore != null &&
                    (charBefore.isDigit() || charBefore == ')') -> "×("
            // 기본: 여는 괄호
            else -> "("
        }

        reduce { buildInsertState(state, textToInsert) }
    }

    private fun handleDelete() = intent {
        val expression = state.expression
        val cursorPos = state.cursorPosition

        if (expression.isEmpty() || cursorPos == 0) return@intent

        val newExpression = StringBuilder(expression)
            .deleteCharAt(cursorPos - 1)
            .toString()

        reduce { buildNewExpressionState(state, newExpression, cursorPos - 1) }
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

        val expressionToCalculate =
            if (state.repeatOperation != null && expression.toDoubleOrNull() != null) {
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
                    UiText.StringResource(R.string.invalid_expression)
                )
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
                )
            )
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Log.e("CalculatorViewModel", "Failed to save calculator history.", e)
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(R.string.calculator_history_save_failed)
                )
            )
        }.isSuccess

        reduce {
            buildNewExpressionState(
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

            val preservedMain = state.mainExchangeCurrency?.let { current ->
                currencies.find { it.code == current.code }
            }

            val mainCurrency = preservedMain
                ?: currencies.find { it.code == savedSelection?.mainCode }
                ?: currencies.find { it.code == deviceCurrencyCode }
                ?: currencies.find { it.code == "KRW" }
                ?: currencies.firstOrNull()

            val preservedSub = state.selectedExchangeCurrency?.let { current ->
                currencies.find { it.code == current.code && it.code != mainCurrency?.code }
            }

            val subCurrency = preservedSub
                ?: currencies.find { it.code == savedSelection?.subCode && it.code != mainCurrency?.code }
                ?: currencies.find { it.code == "USD" && it.code != mainCurrency?.code }
                ?: currencies.firstOrNull { it.code != mainCurrency?.code }

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
                    UiText.StringResource(
                        R.string.load_currency_list_failed,
                        listOf(e.message.orEmpty()),
                    )
                )
            )

            Log.e(
                "CalculatorViewModel",
                "Failed to load currency list.",
                e
            )
        }
    }

    private fun getDeviceCurrencyCode(): String {
        return runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("KRW")
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
                state.copy(exchangeRate = rate).withConvertedAmounts()
            }
        } catch (e: Exception) {
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    UiText.StringResource(
                        R.string.load_exchange_rate_failed,
                        listOf(e.message.orEmpty()),
                    )
                )
            )
        }
    }

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performSwapExchangeCurrencies() {
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

    // ── Pure Computation Helpers (상태를 직접 변경 하지 않음) ───

    private fun buildInsertState(
        currentState: CalculatorContract.State,
        textToInsert: String
    ): CalculatorContract.State {
        val newExpression = StringBuilder(currentState.expression)
            .insert(currentState.cursorPosition, textToInsert)
            .toString()
        val newCursorPos = currentState.cursorPosition + textToInsert.length
        return buildNewExpressionState(currentState, newExpression, newCursorPos)
    }

    private fun buildNewExpressionState(
        currentState: CalculatorContract.State,
        newExpression: String,
        newCursorPos: Int
    ): CalculatorContract.State {
        return currentState.copy(
            expression = newExpression,
            cursorPosition = newCursorPos,
            previewResult = calculatePreview(newExpression),
            repeatOperation = null, // 새 입력이 들어오면 반복 연산 초기화
            isCalculatedResult = false,
            isError = false,
            errorMessage = null
        ).withConvertedAmounts()
    }

    /**
     * 주어진 산술 표현식에 대한 실시간 미리보기 결과를 계산합니다.
     *
     * 표현식이 비어 있거나, 연산자로 끝나거나, 연산자를 포함하지 않거나,
     * 단독 음수만 나타내거나, 평가에 실패한 경우 빈 문자열을 반환합니다.
     *
     * @param expression 미리보기로 계산할 산술 표현식입니다.
     * @return 계산된 결과 문자열 또는 미리보기를 제공할 수 없는 경우 빈 문자열입니다.
     */
    private fun calculatePreview(expression: String): String {
        if (expression.isEmpty()) return ""

        val lastChar = expression.lastOrNull() ?: return ""
        val hasOperator = expression.any { it.toString() in OPERATORS }
        val isJustNegativeNumber = expression.startsWith("-") &&
                expression.count { it.toString() in OPERATORS } == 1

        // 연산자가 없거나, 마지막이 연산자거나, 단순 음수면 미리보기 안 함
        if (!hasOperator || lastChar.toString() in OPERATORS || isJustNegativeNumber) {
            return ""
        }

        val result = calculatorUseCases.calculateExpression.calculate(expression)
        return if (result == "Error") "" else result
    }

    private fun canInsertNumber(expression: String, cursorPos: Int): Boolean {
        val currentNumberBlock = getCurrentNumberBlock(expression, cursorPos)
        return currentNumberBlock.length < MAX_NUMBER_LENGTH
    }

    private fun getCurrentNumberBlock(expression: String, cursorPos: Int): String {
        if (expression.isEmpty()) return ""

        // 커서 앞부분에서 마지막 구분자 찾기
        val textBeforeCursor = expression.take(cursorPos)
        val lastSeparatorIndex = textBeforeCursor.indexOfLast { it in "+-×÷()" }
        val startOfNumber = if (lastSeparatorIndex == -1) 0 else lastSeparatorIndex + 1

        // 커서 뒷부분에서 첫 구분자 찾기
        val textAfterCursor = expression.drop(cursorPos)
        val firstSeparatorIndex = textAfterCursor.indexOfFirst { it in "+-×÷()" }
        val endOfNumber =
            if (firstSeparatorIndex == -1) expression.length else cursorPos + firstSeparatorIndex

        return expression.substring(startOfNumber, endOfNumber)
    }

    private fun CalculatorContract.State.withConvertedAmounts(): CalculatorContract.State {
        return copy(
            convertedExpressionAmount = exchangeUseCases.convertExchangeAmount.convertExpression(
                expression = expression,
                rate = exchangeRate,
                currencyCode = selectedExchangeCurrency?.code
            ),
            convertedPreviewAmount = exchangeUseCases.convertExchangeAmount.convertSingleAmount(
                text = previewResult,
                rate = exchangeRate,
                currencyCode = selectedExchangeCurrency?.code
            ),
        )
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
                    UiText.StringResource(R.string.calculator_history_delete_failed)
                )
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
