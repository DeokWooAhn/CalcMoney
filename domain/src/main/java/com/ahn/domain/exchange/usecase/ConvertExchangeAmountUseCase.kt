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

        var index = 0
        while (index < expression.length) {
            val char = expression[index]
            val exponentEnd = if (char == 'E' && number.isNotEmpty()) exponentEnd(expression, index) else null

            when {
                char.isDigit() || char == '.' -> {
                    number.append(char)
                    index++
                }

                // 큰·작은 계산 결과(1.2193263111E+17)는 지수부까지 한 숫자다. 지수의 부호를 연산자로 나누면
                // "0.00 USD E + 0.01 USD"처럼 조각마다 환산한다.
                exponentEnd != null -> {
                    number.append(expression, index, exponentEnd)
                    index = exponentEnd
                }

                else -> {
                    flushNumber()
                    result.append(" ").append(char).append(" ")
                    index++
                }
            }
        }

        flushNumber()

        return result.toString().replace(MULTI_SPACE_REGEX, " ").trim()
    }

    /**
     * `start`의 `E`부터 지수 숫자 끝까지의 끝 위치. 지수 숫자가 없으면 null.
     * 계산 엔진(`CalculateExpressionUseCase`의 `parseExponent`)과 같은 규칙이다: `E`, 부호(선택), 숫자 한 자리 이상.
     */
    private fun exponentEnd(
        expression: String,
        start: Int,
    ): Int? {
        var index = start + 1
        if (index < expression.length && (expression[index] == '+' || expression[index] == '-')) index++
        val digitsStart = index
        while (index < expression.length && expression[index].isDigit()) index++
        return index.takeIf { it > digitsStart }
    }

    fun convertSingleAmount(
        text: String,
        rate: Double,
        currencyCode: String?,
    ): String {
        if (text.isEmpty() || rate <= 0.0 || currencyCode == null) return ""

        val amount = text.toDoubleOrNull()
            ?: calculateExpressionUseCase(text)
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
