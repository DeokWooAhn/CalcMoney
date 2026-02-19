package com.ahn.data.remote

import com.ahn.data.model.ExchangeRateResponse
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
            parameter("authKey", authKey)
            parameter("searchData", searchData)
            parameter("data", "AP01")
        }.body()
    }
}