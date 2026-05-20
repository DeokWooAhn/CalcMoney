package com.ahn.data.exchange.remote.datasource

import com.ahn.data.BuildConfig
import com.ahn.data.exchange.remote.api.ExchangeRateApi
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import javax.inject.Inject

class ExchangeRateRemoteDataSource @Inject constructor(
    private val api: ExchangeRateApi,
) {
    suspend fun fetchExchangeRates(searchDate: String = ""): List<ExchangeRateResponse> {
        return api.getExchangeRate(
            authKey = BuildConfig.EXCHANGE_API_KEY,
            searchData = searchDate,
        )
    }
}
