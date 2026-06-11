package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.domain.currency.model.CurrencyInfo

private val currencyCountryCodes = mapOf(
    "KRW" to "KR",
    "USD" to "US",
    "JPY" to "JP",
    "EUR" to "EU",
    "CNH" to "CN",
    "GBP" to "GB",
    "AUD" to "AU",
    "CAD" to "CA",
    "CHF" to "CH",
    "HKD" to "HK",
    "AED" to "AE",
    "BHD" to "BH",
    "BND" to "BN",
    "DKK" to "DK",
    "IDR" to "ID",
    "KWD" to "KW",
    "MYR" to "MY",
    "NOK" to "NO",
    "NZD" to "NZ",
    "SAR" to "SA",
    "SEK" to "SE",
    "SGD" to "SG",
    "THB" to "TH",
)

/**
 * 환율 목록에서 지정한 통화 코드의 기준 환율을 찾습니다.
 *
 * `KRW`는 기준 통화이므로 1.0으로 처리합니다.
 *
 * @param code 조회할 통화 코드입니다.
 * @return 통화의 기준 환율입니다. 목록에 없으면 `null`을 반환합니다.
 */
internal fun List<ExchangeRateEntity>.rateOf(code: String): Double? {
    if (code == "KRW") return 1.0
    return firstOrNull { it.code == code }?.baseRate
}

internal fun ExchangeRateEntity.toCurrencyInfo(): CurrencyInfo {
    return CurrencyInfo(
        code = code,
        displayCode = code,
        name = currencyName,
        flagEmoji = getFlagEmoji(code),
    )
}

internal fun krwCurrencyInfo(): CurrencyInfo {
    return CurrencyInfo(
        code = "KRW",
        displayCode = "KRW",
        name = "한국 원",
        flagEmoji = getFlagEmoji("KRW"),
    )
}

/**
 * 세 글자 통화 코드를 해당 국가의 국기 이모지로 변환합니다.
 *
 * @param currencyCode ISO 4217 형식에 가까운 세 글자 통화 코드입니다.
 * @return 지원하는 통화이면 국기 이모지를, 지원하지 않으면 빈 문자열을 반환합니다.
 */
private fun getFlagEmoji(currencyCode: String): String {
    return currencyCountryCodes[currencyCode]?.let(::countryFlag).orEmpty()
}

/**
 * 국가 코드를 지역 표시 기호 조합의 국기 이모지로 변환합니다.
 *
 * @param countryCode 보통 두 글자로 이루어진 국가 코드입니다. 대소문자를 구분하지 않습니다.
 * @return 국가 코드에 해당하는 국기 이모지입니다. 빈 문자열이면 빈 문자열을 반환합니다.
 */
private fun countryFlag(countryCode: String): String {
    return countryCode
        .uppercase()
        .map { char ->
            Character.toChars(0x1F1E6 + (char.code - 'A'.code)).concatToString()
        }.joinToString("")
}
