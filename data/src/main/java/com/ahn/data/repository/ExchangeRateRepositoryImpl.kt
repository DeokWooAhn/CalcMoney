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
        val validRates = rates.filter {
            it.result == 1 && !it.currencyUnit.isNullOrBlank() && !it.baseRate.isNullOrBlank()
        }

        if (validRates.isEmpty()) {
            throw IllegalStateException(apiErrorMessage(rates.firstOrNull()?.result))
        }

        if (from == "KRW") {
            val targetRate = validRates.find { it.currencyUnit?.contains(to) == true }
            val rate = targetRate?.baseRate?.toRateNumber(targetRate.currencyUnit)
                ?: throw IllegalStateException("환율 데이터를 찾을 수 없습니다")
            return 1.0 / rate  // 역수 계산: KRW 기준으로 변환
        }

        if (to == "KRW") {
            val sourceRate = validRates.find { it.currencyUnit?.contains(from) == true }
            return sourceRate?.baseRate?.toRateNumber(sourceRate.currencyUnit)
                ?: throw IllegalStateException("환율 데이터를 찾을 수 없습니다")
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
            return fromRate / toRate  // from 통화의 KRW 가치를 to 통화로 변환
        }

        throw IllegalStateException("환율 데이터를 찾을 수 없습니다")
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
            2 -> "환율 API 요청값이 올바르지 않습니다 (result=2)"
            3 -> "환율 API 인증키(authKey)가 올바르지 않습니다 (result=3)"
            4 -> "환율 API 일일 요청 한도를 초과했습니다 (result=4)"
            else -> "환율 데이터를 찾을 수 없습니다"
        }
    }
}
