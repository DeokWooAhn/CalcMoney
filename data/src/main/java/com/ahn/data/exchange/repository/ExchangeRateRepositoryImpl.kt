package com.ahn.data.exchange.repository

import com.ahn.data.exchange.local.datasource.ExchangeRateLocalDataSource
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.mapper.krwCurrencyInfo
import com.ahn.data.exchange.mapper.rateOf
import com.ahn.data.exchange.mapper.toCurrencyInfo
import com.ahn.data.exchange.mapper.toEntity
import com.ahn.data.exchange.remote.datasource.ExchangeRateRemoteDataSource
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.repository.ExchangeRateRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

class ExchangeRateRepositoryImpl @Inject constructor(
    private val remoteDataSource: ExchangeRateRemoteDataSource,
    private val localDataSource: ExchangeRateLocalDataSource,
) : ExchangeRateRepository {

    companion object {
        private const val CACHE_TTL_MS = 60 * 60 * 1000L
        private const val ERROR_EXCHANGE_RATE_NOT_FOUND = "Exchange rate data not found"
    }

    private val refreshMutex = Mutex()

    /**
     * Obtain the current list of exchange rate entities, using cached values when the cache is still fresh
     * or fetching from the remote data source and updating the cache when necessary.
     *
     * The function ensures only one refresh runs at a time.
     *
     * @return The current list of `ExchangeRateEntity` objects (from cache if fresh, otherwise the freshly fetched-and-cached list).
     * @throws IllegalStateException if the remote response contains no valid rate entries and no cached rates are available.
     * @throws Exception if fetching or storing remote data fails and no cached rates are available; the original error is rethrown.
     */
    private suspend fun fetchRatesIfNeeded(): List<ExchangeRateEntity> {
        return refreshMutex.withLock {
            val now = System.currentTimeMillis()
            val cached = localDataSource.getCachedRates()
            val fetchedAt = localDataSource.getLatestFetchedAt()

            if (cached.isNotEmpty() && now - fetchedAt < CACHE_TTL_MS) {
                return@withLock cached
            }

            runCatching {
                val responses = remoteDataSource.fetchExchangeRates()
                val valid = responses.mapNotNull { it.toEntity(now) }

                if (valid.isEmpty()) {
                    throw IllegalStateException(apiErrorMessage(responses.firstOrNull()?.result))
                }

                localDataSource.replaceRates(valid)
                valid
            }.getOrElse { error ->
                cached.ifEmpty { throw error }
            }
        }
    }

    /**
     * Get the exchange rate from one currency to another.
     *
     * @param from The source currency code (e.g., "USD").
     * @param to The target currency code (e.g., "KRW").
     * @return The multiplier to convert one unit of `from` into units of `to`.
     * @throws IllegalStateException if either currency code is not available in the current rates (message: `ERROR_EXCHANGE_RATE_NOT_FOUND`).
     */
    override suspend fun getExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0

        val rates = fetchRatesIfNeeded()
        val fromRate = rates.rateOf(from) ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)
        val toRate = rates.rateOf(to) ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)

        return fromRate / toRate
    }

    /**
     * Returns the list of supported currencies, ensuring KRW is included.
     *
     * The list contains KRW plus currencies derived from the available exchange rates and is deduplicated by currency `code`.
     *
     * @return A list of `CurrencyInfo` objects including KRW and currencies from the current rate set, with duplicates removed by `code`.
     */
    override suspend fun getSupportedCurrencies(): List<CurrencyInfo> {
        val rates = fetchRatesIfNeeded()

        return (listOf(krwCurrencyInfo()) + rates.map { it.toCurrencyInfo() })
            .distinctBy { it.code }
    }

    /**
     * Map an exchange rate API result code to a user-facing error message.
     *
     * @param resultCode The API's result code from the exchange rate response, or `null` if unavailable.
     * @return Specific message for result codes `2`, `3`, and `4`; otherwise `ERROR_EXCHANGE_RATE_NOT_FOUND`.
     */
    private fun apiErrorMessage(resultCode: Int?): String {
        return when (resultCode) {
            2 -> "Invalid exchange rate API request (result=2)"
            3 -> "Invalid exchange rate API authKey (result=3)"
            4 -> "Exchange rate API daily request limit exceeded (result=4)"
            else -> ERROR_EXCHANGE_RATE_NOT_FOUND
        }
    }
}
