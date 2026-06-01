package com.ahn.data.exchange.remote.datasource

import com.ahn.data.BuildConfig
import com.ahn.data.exchange.remote.api.ExchangeRateApi
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import javax.inject.Inject

class ExchangeRateRemoteDataSource @Inject constructor(private val api: ExchangeRateApi) {
    /**
     * 원격 API에서 환율 목록을 가져옵니다.
     *
     * @param searchDate API의 `searchData` 파라미터로 전달할 조회 날짜입니다. 기본값은 빈 문자열입니다.
     * @return 원격 API가 반환한 환율 응답 목록입니다.
     */
    suspend fun fetchExchangeRates(searchDate: String = ""): List<ExchangeRateResponse> {
        return api.getExchangeRate(
            authKey = BuildConfig.EXCHANGE_API_KEY,
            searchData = searchDate,
        )
    }
}
