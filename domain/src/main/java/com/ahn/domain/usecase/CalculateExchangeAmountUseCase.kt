package com.ahn.domain.usecase

import java.util.Locale
import javax.inject.Inject

class CalculateExchangeAmountUseCase @Inject constructor() {
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