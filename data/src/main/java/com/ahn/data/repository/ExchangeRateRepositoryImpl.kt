package com.ahn.data.repository

import com.ahn.data.remote.ExchangeRateApi
import com.ahn.domain.exchange.repository.ExchangeRateRepository
import javax.inject.Inject
import com.ahn.data.BuildConfig
import com.ahn.data.model.ExchangeRateResponse
import com.ahn.domain.currency.model.CurrencyInfo

class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: ExchangeRateApi
) : ExchangeRateRepository {

    companion object {
        const val API_KEY = BuildConfig.EXCHANGE_API_KEY
        private const val CACHE_TTL_MS = 60 * 60 * 1000L
        private const val ERROR_EXCHANGE_RATE_NOT_FOUND = "Exchange rate data not found"
    }

    private var cachedRates: List<ExchangeRateResponse> = emptyList()
    private var cacheTimestamp: Long = 0L

    private suspend fun fetchRatesIfNeeded(): List<ExchangeRateResponse> {
        val now = System.currentTimeMillis()

        if (cachedRates.isNotEmpty() && (now - cacheTimestamp) < CACHE_TTL_MS) {
            return cachedRates
        }

        val rates = api.getExchangeRate(authKey = API_KEY)
        val valid = rates.filter {
            it.result == 1 && !it.currencyUnit.isNullOrBlank() && !it.baseRate.isNullOrBlank()
        }

        if (valid.isEmpty()) {
            throw IllegalStateException(apiErrorMessage(rates.firstOrNull()?.result))
        }

        cachedRates = valid
        cacheTimestamp = now

        return valid
    }

    override suspend fun getExchangeRate(from: String, to: String): Double {
        val validRates = fetchRatesIfNeeded()

        if (from == "KRW") {
            val targetRate = validRates.find { it.currencyUnit?.contains(to) == true }
            val rate = targetRate?.baseRate?.toRateNumber(targetRate.currencyUnit)
                ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)
            return 1.0 / rate
        }

        if (to == "KRW") {
            val sourceRate = validRates.find { it.currencyUnit?.contains(from) == true }
            return sourceRate?.baseRate?.toRateNumber(sourceRate.currencyUnit)
                ?: throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)
        }

        val fromRate =
            validRates.find { it.currencyUnit?.contains(from) == true }?.let {
                it.baseRate?.toRateNumber(it.currencyUnit)
            }

        val toRate =
            validRates.find { it.currencyUnit?.contains(to) == true }?.let {
                it.baseRate?.toRateNumber(it.currencyUnit)
            }

        if (fromRate != null && toRate != null) {
            return fromRate / toRate
        }

        throw IllegalStateException(ERROR_EXCHANGE_RATE_NOT_FOUND)
    }

    override suspend fun getSupportedCurrencies(): List<CurrencyInfo> {
        val rates = fetchRatesIfNeeded()

        return rates.map { response ->
            val code = response.currencyUnit!!.replace("(100)", "").trim()

            CurrencyInfo(
                code = code,
                displayCode = code,
                name = response.currencyName ?: "Unknown",
                flagEmoji = getFlagEmoji(code)
            )
        }.distinctBy { it.code }
    }


    private fun getFlagEmoji(currencyCode: String): String {
        return when (currencyCode) {
            "KRW" -> "🇰🇷"
            "USD" -> "🇺🇸"
            "JPY" -> "🇯🇵"
            "EUR" -> "🇪🇺"
            "CNH" -> "🇨🇳"
            "GBP" -> "🇬🇧"
            "AUD" -> "🇦🇺"
            "CAD" -> "🇨🇦"
            "CHF" -> "🇨🇭"
            "HKD" -> "🇭🇰"
            "AED" -> "🇦🇪"
            "BHD" -> "🇧🇭"
            "BND" -> "🇧🇳"
            "DKK" -> "🇩🇰"
            "IDR" -> "🇮🇩"
            "KWD" -> "🇰🇼"
            "MYR" -> "🇲🇾"
            "NOK" -> "🇳🇴"
            "NZD" -> "🇳🇿"
            "SAR" -> "🇸🇦"
            "SEK" -> "🇸🇪"
            "SGD" -> "🇸🇬"
            "THB" -> "🇹🇭"
            else -> "🏳️"
        }
    }

    private fun String.toRateNumber(currencyUnit: String? = null): Double? {
        val number = replace(",", "").toDoubleOrNull() ?: return null
        // JPY, IDR 등 (100) 단위 통화는 1단위로 변환
        return if (currencyUnit?.contains("(100)") == true) {
            number / 100.0
        } else {
            number
        }
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
