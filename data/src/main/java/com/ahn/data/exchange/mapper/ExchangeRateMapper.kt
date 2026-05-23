package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import com.ahn.domain.currency.model.CurrencyInfo

/**
 * 유효한 원격 환율 응답을 로컬 캐시에 저장할 [ExchangeRateEntity]로 변환합니다.
 *
 * `result`가 1이 아니거나, `currencyUnit` 또는 `baseRate`가 비어 있거나,
 * `baseRate`를 숫자로 파싱할 수 없으면 `null`을 반환합니다.
 *
 * @param fetchedAt 환율을 가져온 시각입니다. Unix epoch 기준 밀리초입니다.
 * @return 정규화된 통화 코드, 통화 단위, 통화명, 기준 환율, 조회 시각을 가진 엔티티입니다.
 *         응답이 유효하지 않거나 파싱에 실패하면 `null`을 반환합니다.
 */
internal fun ExchangeRateResponse.toEntity(fetchedAt: Long): ExchangeRateEntity? {
    if (result != 1 || currencyUnit.isNullOrBlank() || baseRate.isNullOrBlank()) return null

    val unit = currencyUnit.trim()
    val code = unit.replace("(100)", "").trim()
    val rate = baseRate.toRateNumber(unit) ?: return null

    return ExchangeRateEntity(
        code = code,
        currencyUnit = unit,
        currencyName = currencyName ?: "Unknown",
        baseRate = rate,
        fetchedAt = fetchedAt,
    )
}

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
 * 환율 문자열을 숫자로 파싱하고, 통화 단위에 따라 필요한 배율을 적용합니다.
 *
 * 통화 단위에 `"(100)"`이 포함되어 있으면 API가 100단위 환율을 내려준 것이므로
 * 파싱한 값을 100으로 나눕니다.
 *
 * @param currencyUnit `(100)` 단위 여부를 확인할 통화 단위 문자열입니다.
 * @return 파싱된 환율입니다. 숫자로 변환할 수 없으면 `null`을 반환합니다.
 */
private fun String.toRateNumber(currencyUnit: String? = null): Double? {
    val number = replace(",", "").toDoubleOrNull() ?: return null
    return if (currencyUnit?.contains("(100)") == true) {
        number / 100.0
    } else {
        number
    }
}

/**
 * 세 글자 통화 코드를 해당 국가의 국기 이모지로 변환합니다.
 *
 * @param currencyCode ISO 4217 형식에 가까운 세 글자 통화 코드입니다.
 * @return 지원하는 통화이면 국기 이모지를, 지원하지 않으면 빈 문자열을 반환합니다.
 */
private fun getFlagEmoji(currencyCode: String): String {
    val countryCode = when (currencyCode) {
        "KRW" -> "KR"
        "USD" -> "US"
        "JPY" -> "JP"
        "EUR" -> "EU"
        "CNH" -> "CN"
        "GBP" -> "GB"
        "AUD" -> "AU"
        "CAD" -> "CA"
        "CHF" -> "CH"
        "HKD" -> "HK"
        "AED" -> "AE"
        "BHD" -> "BH"
        "BND" -> "BN"
        "DKK" -> "DK"
        "IDR" -> "ID"
        "KWD" -> "KW"
        "MYR" -> "MY"
        "NOK" -> "NO"
        "NZD" -> "NZ"
        "SAR" -> "SA"
        "SEK" -> "SE"
        "SGD" -> "SG"
        "THB" -> "TH"
        else -> return ""
    }

    return countryFlag(countryCode)
}

/**
 * 국가 코드를 지역 표시 기호 조합의 국기 이모지로 변환합니다.
 *
 * @param countryCode 보통 두 글자로 이루어진 국가 코드입니다. 대소문자를 구분하지 않습니다.
 * @return 국가 코드에 해당하는 국기 이모지입니다. 빈 문자열이면 빈 문자열을 반환합니다.
 */
private fun countryFlag(countryCode: String): String {
    return countryCode.uppercase().map { char ->
        Character.toChars(0x1F1E6 + (char.code - 'A'.code)).concatToString()
    }.joinToString("")
}
