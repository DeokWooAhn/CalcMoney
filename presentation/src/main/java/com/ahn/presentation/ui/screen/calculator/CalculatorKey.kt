package com.ahn.presentation.ui.screen.calculator

internal sealed interface CalculatorKey {
    data object History : CalculatorKey

    data object Clear : CalculatorKey

    data object Parenthesis : CalculatorKey

    data object Dot : CalculatorKey

    data object Delete : CalculatorKey

    data object Calculate : CalculatorKey

    data class Number(val value: String) : CalculatorKey

    data class Operator(
        val displayText: String,
        val inputValue: String,
    ) : CalculatorKey
}

internal val calculatorKeyRows = listOf(
    listOf(
        CalculatorKey.History,
        CalculatorKey.Clear,
        CalculatorKey.Parenthesis,
        CalculatorKey.Operator(displayText = "÷", inputValue = "÷"),
    ),
    listOf(
        CalculatorKey.Number("7"),
        CalculatorKey.Number("8"),
        CalculatorKey.Number("9"),
        CalculatorKey.Operator(displayText = "×", inputValue = "×"),
    ),
    listOf(
        CalculatorKey.Number("4"),
        CalculatorKey.Number("5"),
        CalculatorKey.Number("6"),
        CalculatorKey.Operator(displayText = "−", inputValue = "-"),
    ),
    listOf(
        CalculatorKey.Number("1"),
        CalculatorKey.Number("2"),
        CalculatorKey.Number("3"),
        CalculatorKey.Operator(displayText = "+", inputValue = "+"),
    ),
    listOf(
        CalculatorKey.Dot,
        CalculatorKey.Number("0"),
        CalculatorKey.Delete,
        CalculatorKey.Calculate,
    ),
)

internal fun CalculatorKey.toIntent(): CalculatorContract.Intent? =
    when (this) {
        CalculatorKey.History -> null

        CalculatorKey.Clear -> CalculatorContract.Intent.Clear

        CalculatorKey.Parenthesis ->
            CalculatorContract.Intent.Input(CalculatorToken.Parenthesis)

        CalculatorKey.Dot -> CalculatorContract.Intent.Input(CalculatorToken.Dot)

        CalculatorKey.Delete -> CalculatorContract.Intent.Delete

        CalculatorKey.Calculate -> CalculatorContract.Intent.Calculate

        is CalculatorKey.Number ->
            CalculatorContract.Intent.Input(CalculatorToken.Number(value))

        is CalculatorKey.Operator ->
            CalculatorContract.Intent.Input(CalculatorToken.Operator(inputValue))
    }

/**
 * E2E 테스트(Maestro)가 키를 찾을 때 쓰는 id. iOS `CalculatorKey.testID`와 값이 같아야
 * Flow 하나로 두 플랫폼을 돌릴 수 있다.
 *
 * 숫자는 그대로, 나머지는 영어 이름으로 짓고 기호(`+`, `=`, `( )`)는 쓰지 않는다. Maestro는 `id:`를
 * 정규식으로 해석해서, `keypad.+`는 "keypad 뒤에 아무 글자나"가 되어 다른 키를 누른다.
 */
internal val CalculatorKey.testTag: String
    get() = "keypad." + when (this) {
        CalculatorKey.History -> "history"
        CalculatorKey.Clear -> "clear"
        CalculatorKey.Parenthesis -> "parenthesis"
        CalculatorKey.Dot -> "dot"
        CalculatorKey.Delete -> "delete"
        CalculatorKey.Calculate -> "equals"
        is CalculatorKey.Number -> value
        is CalculatorKey.Operator -> when (inputValue) {
            "+" -> "plus"
            "-" -> "minus"
            "×" -> "multiply"
            "÷" -> "divide"
            else -> inputValue
        }
    }
