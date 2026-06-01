package com.ahn.presentation.ui.screen.calculator

import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import com.ahn.domain.exchange.usecase.ConvertExchangeAmountUseCase

internal class CalculatorExpressionReducer(
    private val calculateExpression: CalculateExpressionUseCase,
    private val convertExchangeAmount: ConvertExchangeAmountUseCase,
) {
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
            char.toString() in OPERATORS && !expression.isUnaryMinusOperator(index)
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

        val result = calculateExpression.calculate(expression)
        return if (result == "Error") "" else result
    }

    private fun String.isUnaryMinusOperator(index: Int): Boolean {
        if (this[index] != '-') return false

        val previousChar = getOrNull(index - 1)
        return index == 0 || previousChar == '(' || previousChar.toString() in OPERATORS
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
