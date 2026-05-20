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
import kotlinx.coroutines.CancellationException
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
                if (error is CancellationException) throw error
                cached.ifEmpty { throw error }
            }
        }
    }

    override suspend fun getExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0

        val rates = fetchRatesIfNeeded()
        val fromRate = rates.rateOf(from) ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)
        val toRate = rates.rateOf(to) ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)

        return fromRate / toRate
    }

    override suspend fun getSupportedCurrencies(): List<CurrencyInfo> {
        val rates = fetchRatesIfNeeded()

        return (listOf(krwCurrencyInfo()) + rates.map { it.toCurrencyInfo() })
            .distinctBy { it.code }
    }

    private fun apiErrorMessage(resultCode: Int?): String {
        return when (resultCode) {
            2 -> "Invalid exchange rate API request (result=2)"
            3 -> "Invalid exchange rate API authKey (result=3)"
            4 -> "Exchange rate API daily request limit exceeded (result=4)"
            else -> ERROR_EXCHANGE_RATE_NOT_FOUND
        }
    }
}
