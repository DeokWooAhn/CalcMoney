package com.ahn.presentation.ui.screen.calculator

import android.util.Log
import androidx.lifecycle.ViewModel
import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.usecase.CalculateExpressionUseCase
import com.ahn.domain.usecase.ConvertExchangeAmountUseCase
import com.ahn.domain.usecase.ExtractRepeatOperationUseCase
import com.ahn.domain.usecase.GetExchangeRateUseCase
import com.ahn.domain.usecase.GetSupportedCurrenciesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculateExpressionUseCase: CalculateExpressionUseCase,
    private val getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase,
    private val getExchangeRateUseCase: GetExchangeRateUseCase,
    private val convertExchangeAmountUseCase: ConvertExchangeAmountUseCase,
    private val extractRepeatOperationUseCase: ExtractRepeatOperationUseCase,
) : ViewModel(), ContainerHost<CalculatorContract.State, CalculatorContract.SideEffect> {

    override val container = container(
        initialState = CalculatorContract.State()
    ) {
        performLoadCurrencies()
    }

    companion object {
        private const val MAX_NUMBER_LENGTH = 15
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
            is CalculatorContract.Intent.SelectExchangeCurrency -> handleSelectExchangeCurrency(
                intent.currency
            )

            is CalculatorContract.Intent.Delete -> handleDelete()
            is CalculatorContract.Intent.Clear -> handleClear()
            is CalculatorContract.Intent.Calculate -> handleCalculate()
            is CalculatorContract.Intent.SelectMainExchangeCurrency -> handleSelectMainExchangeCurrency(
                intent.currency
            )

            CalculatorContract.Intent.SwapExchangeCurrencies -> intent { performSwapExchangeCurrencies() }
            CalculatorContract.Intent.ClearHistory -> handleClearHistory()
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

        val result = calculateExpressionUseCase.calculate(expressionToCalculate)

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

        val nextRepeatOperation = extractRepeatOperationUseCase(expressionToCalculate)
            ?: state.repeatOperation

        val historyItem = CalculatorContract.HistoryItem(
            expression = expressionToCalculate,
            result = result,
        )

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
                histories = state.histories + historyItem,
            )
        }
    }

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performLoadCurrencies() {
        try {
            val currencies = getSupportedCurrenciesUseCase()
            val mainCurrency = currencies.find { it.code == "KRW" } ?: currencies.firstOrNull()
            val subCurrency = currencies.find { it.code == "USD" }
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

    private suspend fun Syntax<CalculatorContract.State, CalculatorContract.SideEffect>.performFetchExchangeRate() {
        val from = state.mainExchangeCurrency ?: return
        val to = state.selectedExchangeCurrency ?: return

        try {
            val rate = if (from.code == to.code) {
                1.0
            } else {
                getExchangeRateUseCase(from = from.code, to = to.code)
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
     * Compute a live-preview result for the given arithmetic expression.
     *
     * Returns an empty string when the expression is empty, ends with an operator, contains no operator,
     * represents a lone negative number, or when evaluation fails.
     *
     * @param expression The arithmetic expression to evaluate for preview.
     * @return The evaluated result as a string, or an empty string if no preview is available.
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

        val result = calculateExpressionUseCase.calculate(expression)
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
            convertedExpressionAmount = convertExchangeAmountUseCase.convertExpression(
                expression = expression,
                rate = exchangeRate,
                currencyCode = selectedExchangeCurrency?.code
            ),
            convertedPreviewAmount = convertExchangeAmountUseCase.convertSingleAmount(
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

    private fun handleClearHistory() = intent {
        reduce { state.copy(histories = emptyList()) }
    }
}
