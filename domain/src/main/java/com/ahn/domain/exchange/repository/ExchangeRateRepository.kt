package com.ahn.domain.exchange.repository

import com.ahn.domain.currency.model.CurrencyInfo

interface ExchangeRateRepository {
    /**
 * Fetches the exchange rate from one currency to another.
 *
 * @param from Source currency code (e.g., ISO 4217).
 * @param to Target currency code (e.g., ISO 4217).
 * @return The multiplier that converts one unit of `from` into units of `to`.
 */
suspend fun getExchangeRate(from: String, to: String): Double
    suspend fun getSupportedCurrencies(): List<CurrencyInfo>
}