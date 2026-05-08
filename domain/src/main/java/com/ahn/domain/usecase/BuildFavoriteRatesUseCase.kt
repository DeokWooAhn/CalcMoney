package com.ahn.domain.usecase

import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.model.FavoriteRateInfo
import javax.inject.Inject

class BuildFavoriteRatesUseCase @Inject constructor() {
    /**
     * Builds a list of favorite currency rate entries based on the provided base currency, base amount, available currencies, and rates.
     *
     * If `baseCurrency` is `null`, or `favoriteCurrencyCodes` or `availableCurrencies` is empty, this returns an empty list.
     *
     * @param baseCurrency The base currency to convert from; may be `null`.
     * @param baseAmount The amount in the base currency as a string; non-parsable values are treated as zero.
     * @param favoriteCurrencyCodes The ordered list of favorite currency codes to produce entries for.
     * @param availableCurrencies The list of available `CurrencyInfo` objects used to resolve currency details.
     * @param ratesByCode A map of currency code to exchange rate relative to the base currency.
     * @return A list of `FavoriteRateInfo` for each favorite code that is not the base currency and has both a matching `CurrencyInfo` in `availableCurrencies` and a rate in `ratesByCode`; entries without matching data are omitted.
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

        return favoriteCurrencyCodes.mapNotNull { code ->
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