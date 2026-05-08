package com.ahn.domain.usecase

import java.util.Locale
import javax.inject.Inject

class CalculateExchangeAmountUseCase @Inject constructor() {
    /**
     * Calculates the exchanged amount from a string input and returns it formatted to two decimal places using US locale.
     *
     * @param fromAmount String representation of the source amount; if it cannot be parsed to a number it is treated as 0.0.
     * @param rate Exchange rate to multiply the parsed amount by.
     * @return The product formatted with exactly two decimal places (Locale.US) if greater than zero, or an empty string otherwise.
     */
    operator fun invoke(
        fromAmount: String,
        rate: Double,
    ): String {
        val amount = fromAmount.toDoubleOrNull() ?: 0.0
        val result = amount * rate

        return if (result > 0) {
            String.format(Locale.US, "%.2f", result)
        } else {
            ""
        }
    }
}