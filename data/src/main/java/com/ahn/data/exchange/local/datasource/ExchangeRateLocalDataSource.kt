package com.ahn.data.exchange.local.datasource

import com.ahn.data.exchange.local.dao.ExchangeRateDao
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import javax.inject.Inject

class ExchangeRateLocalDataSource @Inject constructor(
    private val dao: ExchangeRateDao,
) {
    /**
     * Retrieves all cached exchange rate entities from local storage.
     *
     * @return A list of stored [ExchangeRateEntity] objects, or an empty list if none are stored.
     */
    suspend fun getCachedRates(): List<ExchangeRateEntity> {
        return dao.getAll()
    }

    /**
     * Retrieve the timestamp when exchange rates were last fetched from local storage.
     *
     * @return The latest fetched-at timestamp in milliseconds since the Unix epoch, or `0L` if no timestamp is stored.
     */
    suspend fun getLatestFetchedAt(): Long {
        return dao.getLatestFetchedAt() ?: 0L
    }

    /**
     * Replaces all cached exchange rates with the provided list.
     *
     * @param rates The list of exchange rate entities to store; existing cached entries will be replaced.
     */
    suspend fun replaceRates(rates: List<ExchangeRateEntity>) {
        dao.replaceAll(rates)
    }
}
