package com.ahn.presentation.ui.screen.calculator

import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import com.ahn.domain.calculator.usecase.ExtractRepeatOperationUseCase
import com.ahn.domain.exchange.usecase.ConvertExchangeAmountUseCase
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText

internal class CalculatorExpressionReducer(
    private val calculateExpression: CalculateExpressionUseCase,
    private val convertExchangeAmount: ConvertExchangeAmountUseCase,
    private val extractRepeatOperation: ExtractRepeatOperationUseCase,
) {
    /**
     * "="를 눌렀을 때의 다음 상태와 저장할 기록을 정한다.
     *
     * 상태를 바꾸거나 저장하지 않는 순수 함수다. ViewModel은 이 함수를 `reduce` 안에서 부르는데, `reduce`는
     * `StateFlow.update`라 다른 변경과 겹치면 블록을 다시 실행한다. 그래도 같은 입력에는 같은 결과가 나온다.
     *
     * 숫자만 남은 결과에서 다시 누르면 직전 연산(`repeatOperation`)을 이어 붙여 계산한다.
     */
    fun calculate(currentState: CalculatorContract.State): CalculationOutcome {
        val expression = currentState.expression
        if (expression.isEmpty()) return CalculationOutcome.Ignored

        val repeatOperation = currentState.repeatOperation
        val shouldRepeatOperation = repeatOperation != null && expression.toDoubleOrNull() != null
        if (!shouldRepeatOperation && !hasBinaryOperator(expression)) return CalculationOutcome.Ignored

        val expressionToCalculate = if (shouldRepeatOperation) expression + repeatOperation else expression
        val result = calculateExpression(expressionToCalculate)

        if (result == "Error") {
            return CalculationOutcome.Failed(
                currentState.copy(
                    isError = true,
                    errorMessage = UiText.StringResource(R.string.calculator_error),
                ),
            )
        }

        val nextState = buildNewExpressionState(
            currentState = currentState,
            newExpression = result,
            newCursorPos = result.length,
        ).copy(
            previewResult = "",
            convertedPreviewAmount = "",
            repeatOperation = extractRepeatOperation(expressionToCalculate) ?: repeatOperation,
            isCalculatedResult = true,
        )

        return CalculationOutcome.Succeeded(
            state = nextState,
            history = CalculatorContract.HistoryItem(expression = expressionToCalculate, result = result),
        )
    }

    fun inputNumber(
        currentState: CalculatorContract.State,
        number: String,
    ): CalculatorExpressionResult {
        val expression = currentState.expression
        val cursorPosition = currentState.cursorPosition

        if (!canInsertNumber(expression, cursorPosition)) {
            return CalculatorExpressionResult.MaxNumberLengthExceeded
        }

        if (expression == "0" && number != "0") {
            return CalculatorExpressionResult.Updated(
                buildNewExpressionState(currentState, number, number.length),
            )
        }

        return CalculatorExpressionResult.Updated(buildInsertState(currentState, number))
    }

    fun inputOperator(
        currentState: CalculatorContract.State,
        operator: String,
    ): CalculatorContract.State? {
        val expression = currentState.expression
        val cursorPosition = currentState.cursorPosition

        if (expression.isEmpty() && operator != "-") return null
        if (cursorPosition == 0 && operator != "-") return null

        val charBefore = expression.getOrNull(cursorPosition - 1)

        return when {
            charBefore != null && charBefore.toString() in OPERATORS && operator == "-" && charBefore != '-' -> {
                buildInsertState(currentState, operator)
            }

            charBefore != null && charBefore.toString() in OPERATORS -> {
                val newExpression = StringBuilder(expression)
                    .deleteCharAt(cursorPosition - 1)
                    .insert(cursorPosition - 1, operator)
                    .toString()

                buildNewExpressionState(currentState, newExpression, cursorPosition)
            }

            charBefore == '(' && operator != "-" -> null

            else -> buildInsertState(currentState, operator)
        }
    }

    fun inputDot(currentState: CalculatorContract.State): CalculatorContract.State? {
        val expression = currentState.expression
        val cursorPosition = currentState.cursorPosition

        if (expression.isEmpty()) {
            return buildNewExpressionState(currentState, "0.", 2)
        }

        val currentNumberBlock = getCurrentNumberBlock(expression, cursorPosition)
        if (currentNumberBlock.contains('.')) return null

        val charBefore = expression.getOrNull(cursorPosition - 1)
        if (charBefore != null && (charBefore.toString() in OPERATORS || charBefore == '(')) {
            return buildInsertState(currentState, "0.")
        }

        return buildInsertState(currentState, ".")
    }

    fun inputParenthesis(currentState: CalculatorContract.State): CalculatorContract.State {
        val expression = currentState.expression
        val cursorPosition = currentState.cursorPosition
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val charBefore = if (cursorPosition > 0) expression.getOrNull(cursorPosition - 1) else null

        val textToInsert = when {
            openCount > closeCount && charBefore != null &&
                    (charBefore.isDigit() || charBefore == ')') -> ")"

            charBefore != null &&
                    (charBefore.isDigit() || charBefore == ')') -> "×("

            else -> "("
        }

        return buildInsertState(currentState, textToInsert)
    }

    fun delete(currentState: CalculatorContract.State): CalculatorContract.State? {
        val expression = currentState.expression
        val cursorPosition = currentState.cursorPosition

        if (expression.isEmpty() || cursorPosition == 0) return null

        val newExpression = StringBuilder(expression)
            .deleteCharAt(cursorPosition - 1)
            .toString()

        return buildNewExpressionState(currentState, newExpression, cursorPosition - 1)
    }

    fun buildNewExpressionState(
        currentState: CalculatorContract.State,
        newExpression: String,
        newCursorPos: Int,
    ): CalculatorContract.State {
        return currentState.copy(
            expression = newExpression,
            cursorPosition = newCursorPos,
            previewResult = calculatePreview(newExpression),
            repeatOperation = null,
            isCalculatedResult = false,
            isError = false,
            errorMessage = null,
        ).withConvertedAmounts()
    }

    fun hasBinaryOperator(expression: String): Boolean {
        return expression.indices.any { index ->
            val char = expression[index]
            char.toString() in OPERATORS &&
                !expression.isUnaryMinusOperator(index) &&
                !expression.isExponentSign(index)
        }
    }

    fun CalculatorContract.State.withConvertedAmounts(): CalculatorContract.State {
        return copy(
            convertedExpressionAmount = convertExchangeAmount.convertExpression(
                expression = expression,
                rate = exchangeRate,
                currencyCode = selectedExchangeCurrency?.code,
            ),
            convertedPreviewAmount = convertExchangeAmount.convertSingleAmount(
                text = previewResult,
                rate = exchangeRate,
                currencyCode = selectedExchangeCurrency?.code,
            ),
        )
    }

    private fun buildInsertState(
        currentState: CalculatorContract.State,
        textToInsert: String,
    ): CalculatorContract.State {
        val newExpression = StringBuilder(currentState.expression)
            .insert(currentState.cursorPosition, textToInsert)
            .toString()
        val newCursorPos = currentState.cursorPosition + textToInsert.length
        return buildNewExpressionState(currentState, newExpression, newCursorPos)
    }

    private fun calculatePreview(expression: String): String {
        if (expression.isEmpty()) return ""

        val lastChar = expression.lastOrNull() ?: return ""

        if (!hasBinaryOperator(expression) || lastChar.toString() in OPERATORS) {
            return ""
        }

        val result = calculateExpression(expression)
        return if (result == "Error") "" else result
    }

    private fun String.isUnaryMinusOperator(index: Int): Boolean {
        if (this[index] != '-') return false

        val previousChar = getOrNull(index - 1)
        return index == 0 || previousChar == '(' || previousChar.toString() in OPERATORS
    }

    // 계산 결과 1.0000000000E+16의 +는 숫자의 일부라 이항 연산자로 세지 않는다.
    private fun String.isExponentSign(index: Int): Boolean {
        return this[index] in "+-" && getOrNull(index - 1) == 'E'
    }

    private fun canInsertNumber(expression: String, cursorPos: Int): Boolean {
        val currentNumberBlock = getCurrentNumberBlock(expression, cursorPos)
        return currentNumberBlock.length < MAX_NUMBER_LENGTH
    }

    private fun getCurrentNumberBlock(expression: String, cursorPos: Int): String {
        if (expression.isEmpty()) return ""

        val textBeforeCursor = expression.take(cursorPos)
        val lastSeparatorIndex = textBeforeCursor.indexOfLast { it in "+-×÷()" }
        val startOfNumber = if (lastSeparatorIndex == -1) 0 else lastSeparatorIndex + 1

        val textAfterCursor = expression.drop(cursorPos)
        val firstSeparatorIndex = textAfterCursor.indexOfFirst { it in "+-×÷()" }
        val endOfNumber =
            if (firstSeparatorIndex == -1) expression.length else cursorPos + firstSeparatorIndex

        return expression.substring(startOfNumber, endOfNumber)
    }

    private companion object {
        private const val MAX_NUMBER_LENGTH = 15
        private val OPERATORS = listOf("+", "-", "×", "÷")
    }
}

internal sealed interface CalculatorExpressionResult {
    data class Updated(val state: CalculatorContract.State) : CalculatorExpressionResult

    data object MaxNumberLengthExceeded : CalculatorExpressionResult
}

/** [CalculatorExpressionReducer.calculate]의 결과. 화면에 반영할 상태와, 성공했을 때 저장할 기록을 담는다. */
internal sealed interface CalculationOutcome {
    /** 계산할 수식이 아니다. 상태를 바꾸지 않는다. */
    data object Ignored : CalculationOutcome

    /** 계산할 수 없는 수식이다. 오류 상태를 보여 주고 기록에 남기지 않는다. */
    data class Failed(val state: CalculatorContract.State) : CalculationOutcome

    /** 계산에 성공했다. [state]를 바로 보여 주고, [history]는 저장에 성공한 뒤 기록 목록에 넣는다. */
    data class Succeeded(
        val state: CalculatorContract.State,
        val history: CalculatorContract.HistoryItem,
    ) : CalculationOutcome
}
