package com.ahn.domain.usecase

import javax.inject.Inject
import kotlin.math.roundToLong

class ConvertExchangeAmountUseCase @Inject constructor(
    private val calculateExpressionUseCase: CalculateExpressionUseCase,
) {
    /**
     * Converts every contiguous numeric token in `expression` by multiplying it by `rate` and appending `currencyCode`, while preserving non-numeric characters and normalizing whitespace.
     *
     * @param expression The input text containing numbers and other characters; numeric substrings (digits and `.`) are detected and converted.
     * @param rate The multiplier applied to each detected numeric substring; must be greater than 0.
     * @param currencyCode The currency code appended after each converted number; if `null` conversion is aborted.
     * @return A single-line string where each parsed number is replaced by the rounded converted value followed by a space and `currencyCode`; returns an empty string when `expression` is empty, `rate` is not greater than 0, or `currencyCode` is `null`.
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

        return result.toString().replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Convert a single amount string to the target currency using the provided exchange rate.
     *
     * Attempts to parse `text` as a numeric value; if parsing fails, evaluates `text` as an expression via
     * the injected expression calculator. If a valid numeric amount is obtained, multiplies it by `rate`,
     * rounds the result to a `Long`, and returns it followed by the `currencyCode` (e.g. "123 USD").
     *
     * @param text The input amount as a plain number or an arithmetic expression.
     * @param rate The multiplier applied to the parsed amount (must be > 0.0).
     * @param currencyCode The currency code to append to the converted value; when `null` the function returns an empty string.
     * @return A string in the format "`<rounded> <currencyCode>`" for a successful conversion, or an empty string if inputs are invalid or the amount cannot be resolved to a number.
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