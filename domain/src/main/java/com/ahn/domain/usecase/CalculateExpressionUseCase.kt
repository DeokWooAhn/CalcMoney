package com.ahn.domain.usecase

import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class CalculateExpressionUseCase @Inject constructor() {
    /**
     * Normalize and evaluate a user arithmetic expression and return the result as a formatted string.
     *
     * The input is normalized (whitespace/newlines removed, `×`→`*`, `÷`→`/`, `−`→`-`), a trailing operator is dropped if present, and the remaining expression is evaluated.
     *
     * @param expression The arithmetic expression to evaluate; may include digits, `.`, `+`, `-`, `*`, `/`, parentheses, and localized operator symbols (`×`, `÷`, `−`), as well as spaces or newlines.
     * @return The evaluation result as:
     *         - `"0"` if the expression is empty after normalization,
     *         - `"Error"` if evaluation fails,
     *         - otherwise a numeric string formatted as an integer when the result is whole, a decimal with up to 10 fractional digits (trailing zeros removed) for typical magnitudes, or scientific notation with 10 decimal digits for magnitudes >= 1e15 or < 1e-10 (excluding zero).
     */
    fun calculate(expression: String): String {
        return try {
            val normalized = expression
                .replace("\n", "")
                .replace(" ", "")
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")

            if (normalized.isEmpty()) return "0"

            // 마지막이 연산자면 제거
            val finalExpression = if (normalized.last().toString().matches(Regex("[+\\-*/]"))) {
                normalized.dropLast(1)
            } else {
                normalized
            }

            val result = eval(finalExpression)

            // 15자리를 넘어가거나(10^15), 너무 작으면(10^-10) E 표기법 사용
            val absResult = abs(result)
            if (absResult != 0.0 && (absResult >= 1e15 || absResult < 1e-10)) {
                // 예: 1.2345678900E15 (소수점 10자리)
                String.Companion.format(Locale.US, "%.10E", result).replace(",", ".")
            } else if (result % 1.0 == 0.0) {
                String.Companion.format(Locale.US, "%.0f", result)
            } else {
                String.Companion.format(Locale.US, "%.10f", result).trimEnd('0').trimEnd('.')
            }
        } catch (_: Exception) {
            "Error"
        }
    }

    private fun eval(expr: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                return x
            }
        }.parse()
    }
}