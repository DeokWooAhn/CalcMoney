package com.ahn.presentation.ui.screen.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahn.domain.usecase.CalculatorEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorEngine: CalculatorEngine
) : ViewModel() {

    private val _state = MutableStateFlow(CalculatorContract.State())
    val state: StateFlow<CalculatorContract.State> = _state.asStateFlow()

    private val _sideEffect = Channel<CalculatorContract.SideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()

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

    private fun handleInput(token: CalculatorToken) {
        val currentState = state.value
        val currentExpression = currentState.expression
        val cursorPosition = currentState.cursorPosition

        when (token) {
            is CalculatorToken.Number -> handleNumberInput(token.value, currentExpression, cursorPosition)
            is CalculatorToken.Operator -> handleOperatorInput(token.value, currentExpression, cursorPosition)
            is CalculatorToken.Dot -> handleDotInput(currentExpression, cursorPosition)
            is CalculatorToken.Parenthesis -> handleParenthesisInput(currentExpression, cursorPosition)
        }
    }

    private fun handleNumberInput(number: String, expression: String, cursorPosition: Int) {
        if (!canInsertNumber(expression, cursorPosition)) {
            sendSideEffect(CalculatorContract.SideEffect.ShowToast("숫자는 최대 $MAX_NUMBER_LENGTH 자리까지 입력 가능합니다."))
            return
        }

        if (expression == "0" && number != "0") {
            updateState("", 0, number)
            return
        }

        insertText(expression, cursorPosition, number)
    }

    private fun handleOperatorInput(operator: String, expression: String, cursorPosition: Int) {
        if (expression.isEmpty()) return

        if (cursorPosition == 0) return

        val charBefore = expression.getOrNull(cursorPosition - 1)

        if (charBefore != null && charBefore.toString() in OPERATORS) {
            val newExpression = StringBuilder(expression)
                .deleteCharAt(cursorPosition - 1)
                .insert(cursorPosition - 1, operator)
                .toString()

            updateState(expression, cursorPosition, newExpression, cursorPosition)
            return
        }

        if (charBefore == '(') return

        insertText(expression, cursorPosition, operator)
    }

    private fun handleDotInput(expression: String, cursorPosition: Int) {
        if (expression.isEmpty()) {
            updateState("", 0, "0.", 2)
            return
        }

        val currentNumberBlock = getCurrentNumberBlock(expression, cursorPosition)
        if (currentNumberBlock.contains('.')) return

        val charBefore = expression.getOrNull(cursorPosition - 1)

        if (charBefore != null && (charBefore.toString() in OPERATORS || charBefore == '(')) {
            insertText(expression, cursorPosition, "0.")
            return
        }

        insertText(expression, cursorPosition, ".")
    }

    private fun handleParenthesisInput(expression: String, cursorPosition: Int) {
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val charBefore = if (cursorPosition > 0) expression.getOrNull(cursorPosition - 1) else null

        // 닫는 괄호가 필요한 상황
        if (openCount > closeCount && charBefore != null && (charBefore.isDigit() || charBefore == ')')) {
            insertText(expression, cursorPosition, ")")
            return
        }

        // 숫자나 닫는 괄호 뒤에 여는 괄호면 곱하기 추가
        if (charBefore != null && (charBefore.isDigit() || charBefore == ')')) {
            insertText(expression, cursorPosition, "×(")
            return
        }

        // 기본: 여는 괄호
        insertText(expression, cursorPosition, "(")
    }

    private fun handleDelete() {
        val currentState = _state.value
        val expression = currentState.expression
        val cursorPos = currentState.cursorPosition

        if (expression.isEmpty() || cursorPos == 0) return

        val newExpression = StringBuilder(expression)
            .deleteCharAt(cursorPos - 1)
            .toString()
        val newCursorPos = cursorPos - 1

        updateState(expression, cursorPos, newExpression, newCursorPos)
    }

    private fun handleClear() {
        _state.update {
            CalculatorContract.State()
        }
    }

    private fun handleCalculate() {
        val expression = _state.value.expression

        if (expression.isEmpty()) return

        val result = calculatorEngine.calculate(expression)

        if (result == "Error") {
            _state.update {
                it.copy(
                    isError = true,
                    errorMessage = "계산 오류"
                )
            }
            sendSideEffect(CalculatorContract.SideEffect.ShowToast("계산할 수 없는 수식입니다."))
            return
        }

        _state.update {
            CalculatorContract.State(
                expression = result,
                cursorPosition = result.length,
                previewResult = "",
                isError = false,
                errorMessage = null
            )
        }
    }

    private fun insertText(expression: String, cursorPos: Int, textToInsert: String) {
        val newExpression = StringBuilder(expression)
            .insert(cursorPos, textToInsert)
            .toString()
        val newCursorPos = cursorPos + textToInsert.length

        updateState(expression, cursorPos, newExpression, newCursorPos)
    }

    private fun updateState(
        oldExpression: String,
        oldCursorPos: Int,
        newExpression: String,
        newCursorPos: Int = newExpression.length
    ) {
        val previewResult = calculatePreview(newExpression)

        _state.update {
            it.copy(
                expression = newExpression,
                cursorPosition = newCursorPos,
                previewResult = previewResult,
                isError = false,
                errorMessage = null
            )
        }
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
        val endOfNumber = if (firstSeparatorIndex == -1) expression.length else cursorPos + firstSeparatorIndex

        return expression.substring(startOfNumber, endOfNumber)
    }

    private fun sendSideEffect(effect: CalculatorContract.SideEffect) {
        viewModelScope.launch {
            _sideEffect.send(effect)
        }
    }

    fun updateCursorPosition(newCursorPosition: Int) {
        _state.update {
            it.copy(cursorPosition = newCursorPosition.coerceIn(0, it.expression.length))
        }
    }
}