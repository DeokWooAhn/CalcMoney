package com.ahn.data.repository

import com.ahn.data.remote.ExchangeRateApi
import com.ahn.domain.repository.ExchangeRateRepository
import javax.inject.Inject
import com.ahn.data.BuildConfig

class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: ExchangeRateApi
) : ExchangeRateRepository {

    companion object {
        const val API_KEY = BuildConfig.EXCHANGE_API_KEY
    }

    override suspend fun getExchangeRate(from: String, to: String): Double {
        val rates = api.getExchangeRate(authKey = API_KEY)

        if (from == "KRW") {
            val targetRate = rates.find { it.currencyUnit.contains(to) }
            return targetRate?.baseRate?.replace(",", "")?.toDouble()
                ?: throw IllegalStateException("환율 데이터를 찾을 수 없습니다")
        }

        if (to == "KRW") {
            val sourceRate = rates.find { it.currencyUnit.contains(from) }
            return sourceRate?.baseRate?.replace(",", "")?.toDouble()
                ?: throw IllegalStateException("환율 데이터를 찾을 수 없습니다")
        }

        val fromRate =
            rates.find { it.currencyUnit.contains(from) }?.baseRate?.replace(",", "")?.toDouble()
        val toRate =
            rates.find { it.currencyUnit.contains(to) }?.baseRate?.replace(",", "")?.toDouble()

        if (fromRate != null && toRate != null) {
            return toRate / fromRate
        }

        throw IllegalStateException("환율 데이터를 찾을 수 없습니다")
    }

}