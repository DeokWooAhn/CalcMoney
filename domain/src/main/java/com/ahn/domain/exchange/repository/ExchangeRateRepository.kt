package com.ahn.domain.exchange.repository

import com.ahn.domain.currency.model.CurrencyInfo

interface ExchangeRateRepository {
    suspend fun getExchangeRate(from: String, to: String): Double

    suspend fun getLatestRateDate(): String

    suspend fun getLatestFetchedAt(): Long

    suspend fun refreshExchangeRates()

    suspend fun getSupportedCurrencies(): List<CurrencyInfo>
}
