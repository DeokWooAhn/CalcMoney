package com.ahn.presentation.ui.screen.calculator

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.usecase.CalculatorUseCases
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /**
     * Routes a calculator UI intent to the corresponding handler method.
     *
     * Dispatches each `CalculatorContract.Intent` to its dedicated handler:
     * - `Input` -> `handleInput`
     * - `SelectExchangeCurrency` -> `handleSelectExchangeCurrency`
     * - `SelectMainExchangeCurrency` -> `handleSelectMainExchangeCurrency`
     * - `ToggleFavorite` -> `handleToggleFavorite`
     * - `Delete` -> `handleDelete`
     * - `Clear` -> `handleClear`
     * - `Calculate` -> `handleCalculate`
     * - `SwapExchangeCurrencies` -> `performSwapExchangeCurrencies`
     * - `ClearHistory` -> `handleClearHistory`
     *
     * @param intent The intent to process.
     */
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

    /**
     * Toggles the favorite status of the given currency and posts a snackbar indicating whether it was added or removed.
     *
     * @param currencyCode The ISO currency code to toggle in the favorites list.
     */
    private fun handleToggleFavorite(currencyCode: String) = intent {
        val wasFavorite = currencyCode in state.favoriteCurrencyCodes

        favoriteUseCases.toggleFavoriteCurrency(currencyCode)

        postSideEffect(
            CalculatorContract.SideEffect.ShowSnackBar(
                if (wasFavorite) "즐겨찾기가 해제되었습니다."
                else "즐겨찾기에 추가되었습니다."
            )
        )
    }

    /**
     * Processes a calculator input token and performs the corresponding input action.
     *
     * Handles number tokens by inserting digits, operator tokens by inserting or replacing operators,
     * dot tokens by inserting a decimal point (or `0.` when appropriate), and parenthesis tokens by
     * inserting `(` or `)` according to the current expression context.
     *
     * @param token The calculator input token to handle.
     */
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
                    "숫자는 최대 $MAX_NUMBER_LENGTH 자리까지 입력 가능합니다."
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
                state.copy(isError = true, errorMessage = "계산 오류")
            }

            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar(
                    "계산할 수 없는 수식입니다."
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
            Log.e("CalculatorViewModel", "계산 기록 저장 실패", e)
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar("계산 기록을 저장하지 못했습니다.")
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

    /**
     * Loads supported currencies, chooses sensible defaults for main and selected exchange currencies,
     * updates the UI state with available currencies (preserving any existing selections), and triggers
     * fetching of the current exchange rate.
     *
     * Detailed behavior:
     * - Chooses `mainExchangeCurrency` by preferring the device currency code, then `"KRW"`, then the first available currency.
     * - Chooses `selectedExchangeCurrency` by preferring `"USD"` that differs from the main currency, then the first currency that differs from the main.
     * - Preserves `state.mainExchangeCurrency` and `state.selectedExchangeCurrency` when they are already set.
     * - After updating state, calls `performFetchExchangeRate()` to refresh rates/conversions.
     * - On exception, posts a snackbar side effect with an error message and logs the error.
     */
    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performLoadCurrencies() {
        try {
            val currencies = exchangeUseCases.getSupportedCurrencies()
            val deviceCurrencyCode = getDeviceCurrencyCode()

            val mainCurrency = currencies.find { it.code == deviceCurrencyCode }
                ?: currencies.find { it.code == "KRW" }
                ?: currencies.firstOrNull()

            val subCurrency = currencies.find { it.code == "USD" && it.code != mainCurrency?.code }
                ?: currencies.firstOrNull { it.code != mainCurrency?.code }

            reduce {
                state.copy(
                    availableCurrencies = currencies,
                    mainExchangeCurrency = state.mainExchangeCurrency ?: mainCurrency,
                    selectedExchangeCurrency = state.selectedExchangeCurrency ?: subCurrency,
                )
            }

            performFetchExchangeRate()
        } catch (e: Exception) {
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar("통화 목록을 불러올 수 없습니다: ${e.message}")
            )

            Log.e(
                "CalculatorViewModel",
                "통화 목록을 불러올 수 없습니다.",
                e
            )
        }
    }

    /**
     * Retrieve the device's default ISO 4217 currency code, using "KRW" as a fallback.
     *
     * @return The device's currency code (e.g., "USD"); returns "KRW" if the code cannot be determined.
     */
    private fun getDeviceCurrencyCode(): String {
        return runCatching {
            Currency.getInstance(Locale.getDefault()).currencyCode
        }.getOrDefault("KRW")
    }

    /**
     * Fetches the exchange rate for the currently selected main and sub currencies and updates the UI state with the rate and converted amounts.
     *
     * If the selected currencies are the same, sets the rate to `1.0`. On failure posts a snackbar side effect containing the error message.
     */
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
                CalculatorContract.SideEffect.ShowSnackBar("환율 정보를 가져올 수 없습니다: ${e.message}")
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

    /**
     * Create a new UI state with the given expression and cursor position, recalculating derived fields.
     *
     * @param currentState The current calculator UI state to copy from.
     * @param newExpression The expression string to set on the new state.
     * @param newCursorPos The cursor position index within `newExpression`.
     * @return A new `CalculatorContract.State` with `expression` and `cursorPosition` updated, `previewResult` recalculated, `repeatOperation` cleared, error flags reset, and converted amounts recomputed.
     */
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
     * Compute a preview result for the given arithmetic expression.
     *
     * Returns an empty string when a preview cannot be produced, for example when the
     * expression is empty, ends with an operator, contains no operators, represents
     * only a standalone negative number, or when evaluation fails.
     *
     * @param expression The arithmetic expression to evaluate for preview.
     * @return The computed result as a string, or an empty string if no preview is available.
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

    /**
     * Updates the state's converted expression and preview amounts using the current exchange rate and selected currency.
     *
     * @return A copy of the state with `convertedExpressionAmount` and `convertedPreviewAmount` recalculated based on
     * the state's `expression`, `previewResult`, `exchangeRate`, and `selectedExchangeCurrency`.
     */
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

    /**
     * Selects a new exchange currency, resets exchange-related fields, and fetches the corresponding exchange rate.
     *
     * @param currency The currency to select as the sub/converted currency.
     */
    private fun handleSelectExchangeCurrency(currency: CurrencyInfo) = intent {
        if (currency.code == state.selectedExchangeCurrency?.code) return@intent

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

    /**
     * Deletes all persisted calculator history and clears the in-memory history list in state.
     *
     * On success, updates the view state to an empty history list. On failure, logs the error
     * and posts a snackbar side effect informing the user that history could not be deleted.
     */
    private fun handleClearHistory() = intent {
        runCatching {
            calculatorUseCases.clearHistory()
        }.onSuccess {
            reduce { state.copy(histories = emptyList()) }
        }.onFailure { e ->
            Log.e("CalculatorViewModel", "계산 기록 삭제 실패", e)
            postSideEffect(
                CalculatorContract.SideEffect.ShowSnackBar("계산 기록을 삭제하지 못했습니다.")
            )
        }
    }

    /**
     * Observes persisted calculation history and favorite currency codes and updates the UI state.
     *
     * Collects combined flows of saved histories and favorite currencies, maps persisted history entries
     * to `CalculatorContract.HistoryItem`, and replaces `state.histories` and `state.favoriteCurrencyCodes`
     * with the latest values.
     */
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
