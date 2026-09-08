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
