package com.ahn.domain.exchange.usecase

import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import javax.inject.Inject
import kotlin.math.roundToLong

class ConvertExchangeAmountUseCase @Inject constructor(
    private val calculateExpressionUseCase: CalculateExpressionUseCase,
) {
    private companion object {
        val MULTI_SPACE_REGEX = Regex("\\s+")
    }

    fun convertExpression(
        expression: String,
        rate: Double,
        currencyCode: String?,
    ): String {
        if (expression.isEmpty() || rate <= 0.0 || currencyCode == null) return ""

        val result = StringBuilder()
        val number = StringBuilder()

        fun flushNumber() {
            if (number.isEmpty()) return

            val converted = number.toString().toDoubleOrNull()?.let {
                (it * rate).roundToLong()
            }

            if (converted != null) {
                result.append(converted).append(" ").append(currencyCode)
            } else {
                result.append(number)
            }

            number.clear()
        }

        expression.forEach { char ->
            if (char.isDigit() || char == '.') {
                number.append(char)
            } else {
                flushNumber()
                result.append(" ").append(char).append(" ")
            }
        }

        flushNumber()

        return result.toString().replace(MULTI_SPACE_REGEX, " ").trim()
    }

    /**
     * Converts a numeric value or arithmetic expression provided as text into the target currency using the given rate.
     *
     * @param text A numeric value or arithmetic expression represented as a string.
     * @param rate Conversion multiplier; must be greater than zero.
     * @param currencyCode Target currency code (e.g., "USD"); conversion is not performed if this is null.
     * @return A string in the form "`<amount> <currencyCode>`" where `amount` is the parsed/evaluated value multiplied by `rate` and rounded to the nearest integer, or an empty string if the input is invalid or cannot be parsed/evaluated.
     */
    fun convertSingleAmount(
        text: String,
        rate: Double,
        currencyCode: String?,
    ): String {
        if (text.isEmpty() || rate <= 0.0 || currencyCode == null) return ""

        val amount = text.toDoubleOrNull()
            ?: calculateExpressionUseCase.calculate(text)
                .takeIf { it != "Error" }
                ?.toDoubleOrNull()
            ?: return ""

        return "${(amount * rate).roundToLong()} $currencyCode"
    }
}
