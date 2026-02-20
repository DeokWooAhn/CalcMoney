package com.ahn.domain.repository

interface ExchangeRateRepository {
    suspend fun getExchangeRate(from: String, to: String): Double
}