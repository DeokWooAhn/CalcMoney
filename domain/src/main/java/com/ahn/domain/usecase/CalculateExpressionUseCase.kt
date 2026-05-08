package com.ahn.domain.usecase

import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class CalculateExpressionUseCase @Inject constructor() {
    /**
     * 사용자 산술 표현식을 정규화하고 계산한 뒤, 결과를 포맷된 문자열로 반환합니다.
     *
     * 입력값은 정규화됩니다.
     * 공백과 줄바꿈은 제거되고, `×`는 `*`, `÷`는 `/`, `−`는 `-`로 변환됩니다.
     * 또한 표현식 끝에 연산자가 남아 있다면 해당 연산자는 제거한 뒤, 남은 표현식을 계산합니다.
     *
     * @param expression 계산할 산술 표현식입니다.
     *                   숫자, `.`, `+`, `-`, `*`, `/`, 괄호를 포함할 수 있으며,
     *                   지역화된 연산자 기호(`×`, `÷`, `−`)와 공백 또는 줄바꿈도 포함될 수 있습니다.
     *
     * @return 계산 결과를 다음과 같은 문자열로 반환합니다.
     *         - 정규화 후 표현식이 비어 있으면 `"0"`
     *         - 계산에 실패하면 `"Error"`
     *         - 그 외에는 숫자 문자열
     *           결과가 정수이면 정수 형태로 포맷하고,
     *           일반적인 크기의 소수이면 소수점 이하 최대 10자리까지 표시하되 뒤의 0은 제거합니다.
     *           값의 크기가 1e15 이상이거나 0이 아니면서 1e-10보다 작으면,
     *           소수점 이하 10자리의 과학적 표기법으로 표시합니다.
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