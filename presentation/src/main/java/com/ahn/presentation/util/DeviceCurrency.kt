package com.ahn.presentation.util

import java.util.Currency
import java.util.Locale

/**
 * 기기 지역 설정의 통화 코드를 반환합니다. 계산기와 환율 화면이 처음 열릴 때 기본 통화로 씁니다.
 *
 * 지역이 없거나 통화를 알 수 없는 로케일(예: `en`처럼 나라가 빠진 경우)이면 원화를 씁니다.
 */
internal fun deviceCurrencyCode(locale: Locale = Locale.getDefault()): String {
    return runCatching { Currency.getInstance(locale).currencyCode }.getOrDefault("KRW")
}
