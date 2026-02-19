package com.ahn.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRateResponse(
    @SerialName("result") val result: Int,              // 1 : 성공, 2 : DATA 코드 오류, 3 : 인증 코드 오류, 4 : 일일 제한 횟수 마감
    @SerialName("cur_unit") val currencyUnit: String,   // "USD"
    @SerialName("cur_nm") val currencyName: String,     // "미국 달러"
    @SerialName("deal_bas_r") val baseRate: String,     // "1,343.8" (쉼표 주의!)
    @SerialName("ttb") val ttb: String,                 // 전신환 매입
    @SerialName("tts") val tts: String                  // 전신환 매도
)
