package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.domain.currency.model.CurrencyInfo
import java.util.Locale

/**
 * 통화 코드 앞 두 글자가 국가 코드가 아닌 예외.
 *
 * ISO 4217 통화 코드는 대부분 앞 두 글자가 ISO 3166 국가 코드라서 규칙으로 국기를 만들고,
 * 규칙이 맞지 않는 코드만 여기에 둔다. 여러 나라가 함께 쓰는 `X`로 시작하는 통화(XAF, XOF, XDR 등)는
 * 국기 하나를 고를 수 없어 [NO_COUNTRY_FLAG]를 쓴다.
 */
private val flagCountryCodeOverrides = mapOf(
    // EU는 ISO 3166 국가 코드가 아니지만 유럽연합 국기 이모지가 있다.
    "EUR" to "EU",
    // 네덜란드령 안틸레스(AN)는 해체돼 국기 이모지가 없다. 현재 통용되는 퀴라소를 쓴다.
    "ANG" to "CW",
    // 카리브 길더는 2025년에 ANG를 대체한 후속 통화로 발행처가 같다.
    "XCG" to "CW",
)

/**
 * 국가를 특정할 수 없는 통화에 쓰는 중립 아이콘.
 *
 * 빈 문자열이면 목록에서 국기 자리가 비어 통화 코드와 이름이 다른 줄보다 왼쪽으로 밀린다.
 */
internal const val NO_COUNTRY_FLAG = "🌐"

private val isoCountryCodes: Set<String> by lazy { Locale.getISOCountries().toSet() }

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
 * @return 국가를 특정할 수 있으면 국기 이모지를, 없으면 [NO_COUNTRY_FLAG]를 반환합니다.
 */
internal fun getFlagEmoji(currencyCode: String): String {
    return flagCountryCode(currencyCode.uppercase())?.let(::countryFlag) ?: NO_COUNTRY_FLAG
}

private fun flagCountryCode(currencyCode: String): String? {
    flagCountryCodeOverrides[currencyCode]?.let { return it }
    if (currencyCode.length != CURRENCY_CODE_LENGTH || currencyCode.startsWith("X")) return null

    return currencyCode.take(COUNTRY_CODE_LENGTH).takeIf { it in isoCountryCodes }
}

private const val CURRENCY_CODE_LENGTH = 3
private const val COUNTRY_CODE_LENGTH = 2

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
