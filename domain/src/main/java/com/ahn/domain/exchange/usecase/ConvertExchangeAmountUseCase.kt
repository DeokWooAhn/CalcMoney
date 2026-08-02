package com.ahn.domain.exchange.usecase

import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import java.util.Locale
import javax.inject.Inject

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

            val converted = number.toString().toDoubleOrNull()?.takeIf { it.isFinite() }?.let { amount ->
                (amount * rate).takeIf { it.isFinite() }?.let(::formatConvertedAmount)
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

    fun convertSingleAmount(
        text: String,
        rate: Double,
        currencyCode: String?,
    ): String {
        if (text.isEmpty() || rate <= 0.0 || currencyCode == null) return ""

        val amount = text.toDoubleOrNull()
            ?: calculateExpressionUseCase
                .calculate(text)
                .takeIf { it != "Error" }
            ?.toDoubleOrNull()
            ?: return ""

        val convertedAmount = amount
            .takeIf { it.isFinite() }
            ?.times(rate)
            ?.takeIf { it.isFinite() }
            ?: return ""

        return "${formatConvertedAmount(convertedAmount)} $currencyCode"
    }

    /**
     * 계산기 환산 결과는 정수로 반올림하지 않고 소수점 둘째 자리까지 표시합니다.
     * 예를 들어 1,300 KRW를 1 USD = 1,441.10 KRW로 환산하면 0.90 USD가 됩니다.
     */
    private fun formatConvertedAmount(amount: Double): String {
        return String.format(Locale.US, "%.2f", amount)
    }
}
