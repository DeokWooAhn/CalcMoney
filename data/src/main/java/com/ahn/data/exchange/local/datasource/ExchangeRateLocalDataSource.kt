package com.ahn.data.exchange.local.datasource

import com.ahn.data.exchange.local.dao.ExchangeRateDao
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import javax.inject.Inject

class ExchangeRateLocalDataSource @Inject constructor(
    private val dao: ExchangeRateDao,
) {
    suspend fun getCachedRates(): List<ExchangeRateEntity> {
        return dao.getAll()
    }

    suspend fun getLatestFetchedAt(): Long {
        return dao.getLatestFetchedAt() ?: 0L
    }

    suspend fun getLatestRateDate(): String {
        return dao.getLatestRateDate().orEmpty()
    }

    suspend fun replaceRates(rates: List<ExchangeRateEntity>) {
        dao.replaceAll(rates)
    }
}
