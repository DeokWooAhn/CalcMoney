package com.ahn.data.exchange.remote.api

import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import javax.inject.Inject

class ExchangeRateApi @Inject constructor(
    private val client: HttpClient
) {
    suspend fun getExchangeRate(
        authKey: String,
        searchData: String = ""
    ): List<ExchangeRateResponse> {
        return client.get("/site/program/financial/exchangeJSON") {
            parameter("authkey", authKey)
            parameter("searchdate", searchData)
            parameter("data", "AP01")
        }.body()
    }
}