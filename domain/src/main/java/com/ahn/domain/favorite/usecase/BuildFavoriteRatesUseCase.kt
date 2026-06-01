package com.ahn.domain.favorite.usecase

import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.favorite.model.FavoriteRateInfo
import javax.inject.Inject

class BuildFavoriteRatesUseCase @Inject constructor() {
    operator fun invoke(
        baseCurrency: CurrencyInfo?,
        baseAmount: String,
        favoriteCurrencyCodes: List<String>,
        availableCurrencies: List<CurrencyInfo>,
        ratesByCode: Map<String, Double>,
    ): List<FavoriteRateInfo> {
        if (
            baseCurrency == null ||
            favoriteCurrencyCodes.isEmpty() ||
            availableCurrencies.isEmpty()
        ) {
            return emptyList()
        }

        val baseAmountValue = baseAmount.toDoubleOrNull() ?: 0.0
        val currenciesByCode = availableCurrencies.associateBy { it.code }

        return favoriteCurrencyCodes.distinct().mapNotNull { code ->
            if (code == baseCurrency.code) return@mapNotNull null

            val currency = currenciesByCode[code] ?: return@mapNotNull null
            val rate = ratesByCode[code] ?: return@mapNotNull null

            FavoriteRateInfo(
                currency = currency,
                baseCurrencyCode = baseCurrency.code,
                rate = rate,
                convertedAmount = baseAmountValue * rate,
            )
        }
    }
}
