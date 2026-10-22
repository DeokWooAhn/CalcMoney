package com.ahn.domain.calculator.usecase

import javax.inject.Inject

class ExtractRepeatOperationUseCase @Inject constructor() {
    private val operators = setOf('+', '-', '−', '×', '÷')

    operator fun invoke(expression: String): String? {
        val operatorIndex = findLastBinaryOperatorIndex(expression)

        if (operatorIndex <= 0 || operatorIndex == expression.lastIndex) {
            return null
        }

        val operator = expression[operatorIndex].toString()
        val operand = expression.substring(operatorIndex + 1)

        if (!isNumericOperand(operand)) {
            return null
        }

        return operator + operand
    }

    private fun isNumericOperand(operand: String): Boolean {
        return operand.replace('−', '-').toDoubleOrNull() != null
    }

    private fun findLastBinaryOperatorIndex(expression: String): Int {
        return expression.indices.reversed().firstOrNull { index ->
            val current = expression[index]

            if (current !in operators) {
                return@firstOrNull false
            }

            if (index == 0) {
                return@firstOrNull false
            }

            val previous = expression[index - 1]

            // 1.0000000000E+16의 +처럼 지수 표기 안의 부호는 연산자가 아니다.
            previous !in operators && previous != '(' && previous != 'E'
        } ?: -1
    }
}
