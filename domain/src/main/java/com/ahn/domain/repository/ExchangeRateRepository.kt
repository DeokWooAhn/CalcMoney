package com.ahn.domain.repository

import com.ahn.domain.model.CurrencyInfo

interface ExchangeRateRepository {
    suspend fun getExchangeRate(from: String, to: String): Double
    suspend fun getSupportedCurrencies(): List<CurrencyInfo>
}