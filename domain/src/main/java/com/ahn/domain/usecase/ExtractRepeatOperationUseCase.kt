package com.ahn.domain.usecase

import javax.inject.Inject

class ExtractRepeatOperationUseCase @Inject constructor() {
    private val operators = setOf('+', '-', '−', '×', '÷')

    /**
     * Extracts the last binary operator and its right-hand numeric operand from an arithmetic expression.
     *
     * @param expression The arithmetic expression to search.
     * @return A string containing the operator followed by the numeric operand (e.g., "+5.2") if a valid binary operator with a numeric right operand is found; `null` otherwise.
     */
    operator fun invoke(expression: String): String? {
        val operatorIndex = findLastBinaryOperatorIndex(expression)

        if (operatorIndex <= 0 || operatorIndex == expression.lastIndex) {
            return null
        }

        val operator = expression[operatorIndex].toString()
        val operand = expression.substring(operatorIndex + 1)

        if (operand.toDoubleOrNull() == null) {
            return null
        }

        return operator + operand
    }

    /**
     * Finds the index of the last binary operator in an arithmetic expression.
     *
     * An operator is treated as binary if it is one of the supported operator characters
     * and the character immediately before it is neither an operator nor `'('`.
     *
     * @param expression The arithmetic expression to search.
     * @return The index of the last binary operator, or `-1` if no binary operator is found.
     */
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

            previous !in operators && previous != '('
        } ?: -1
    }
}