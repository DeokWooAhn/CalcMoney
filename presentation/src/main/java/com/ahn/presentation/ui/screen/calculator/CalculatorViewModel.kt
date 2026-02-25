package com.ahn.presentation.ui.screen.calculator

import androidx.lifecycle.ViewModel
import com.ahn.domain.usecase.CalculatorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorEngine: CalculatorEngine
) : ViewModel(), ContainerHost<CalculatorContract.State, CalculatorContract.SideEffect> {

    override val container = container<CalculatorContract.State, CalculatorContract.SideEffect>(
        initialState = CalculatorContract.State()
    )

    companion object {
        private const val MAX_NUMBER_LENGTH = 15
        private val OPERATORS = listOf("+", "-", "×", "÷")
    }

    fun processIntent(intent: CalculatorContract.Intent) {
        when (intent) {
            is CalculatorContract.Intent.Input -> handleInput(intent.token)
            is CalculatorContract.Intent.Delete -> handleDelete()
            is CalculatorContract.Intent.Clear -> handleClear()
            is CalculatorContract.Intent.Calculate -> handleCalculate()
        }
    }

    fun updateCursorPosition(newCursorPosition: Int) = intent {
        reduce {
                state.copy(cursorPosition = newCursorPosition.coerceIn(0, state.expression.length))
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
        reduce { CalculatorContract.State() }
    }

    private fun handleCalculate() = intent {
        val expression = state.expression
        if (expression.isEmpty()) return@intent

        val result = calculatorEngine.calculate(expression)

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

        reduce {
            CalculatorContract.State(
                expression = result,
                cursorPosition = result.length,
            )
        }
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
            isError = false,
            errorMessage = null
        )
    }

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

        val result = calculatorEngine.calculate(expression)
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
}