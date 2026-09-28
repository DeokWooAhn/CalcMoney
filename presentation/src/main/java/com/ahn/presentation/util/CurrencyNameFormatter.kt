package com.ahn.presentation.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import java.util.Currency
import java.util.Locale

/**
 * 화면 언어에 맞는 통화 이름을 반환합니다.
 *
 * 직접 다듬은 번역이 있는 통화는 문자열 리소스를, 그 밖의 통화는 플랫폼이 제공하는 현지화 이름을 씁니다.
 * 플랫폼도 모르는 통화(ISO 4217 에 없는 지역 통화 등)는 서버가 준 이름을 그대로 씁니다.
 */
@Composable
fun CurrencyInfo.localizedName(): String {
    val resId = currencyNameResId(code)
    if (resId != null) return stringResource(resId)

    val locale = LocalConfiguration.current.locales[0]
    return platformCurrencyName(code, locale) ?: name
}

/**
 * 플랫폼(ICU)에서 통화 이름을 찾습니다.
 *
 * @return 이름을 모르면 `null`을 반환합니다. 플랫폼은 모르는 통화에 코드를 그대로 돌려주기도 하므로 그 경우도 `null`로 봅니다.
 */
internal fun platformCurrencyName(code: String, locale: Locale): String? {
    val currency = runCatching { Currency.getInstance(code.uppercase(Locale.ROOT)) }.getOrNull()
        ?: return null
    val displayName = currency.getDisplayName(locale)

    return displayName.takeUnless { it.isBlank() || it.equals(code, ignoreCase = true) }
}

private val currencyNameResIds = mapOf(
    "KRW" to R.string.currency_name_krw,
    "USD" to R.string.currency_name_usd,
    "JPY" to R.string.currency_name_jpy,
    "EUR" to R.string.currency_name_eur,
    "CNH" to R.string.currency_name_cnh,
    "GBP" to R.string.currency_name_gbp,
    "AUD" to R.string.currency_name_aud,
    "CAD" to R.string.currency_name_cad,
    "CHF" to R.string.currency_name_chf,
    "HKD" to R.string.currency_name_hkd,
    "AED" to R.string.currency_name_aed,
    "BHD" to R.string.currency_name_bhd,
    "BND" to R.string.currency_name_bnd,
    "DKK" to R.string.currency_name_dkk,
    "IDR" to R.string.currency_name_idr,
    "KWD" to R.string.currency_name_kwd,
    "MYR" to R.string.currency_name_myr,
    "NOK" to R.string.currency_name_nok,
    "NZD" to R.string.currency_name_nzd,
    "SAR" to R.string.currency_name_sar,
    "SEK" to R.string.currency_name_sek,
    "SGD" to R.string.currency_name_sgd,
    "THB" to R.string.currency_name_thb,
)

@StringRes
private fun currencyNameResId(code: String): Int? {
    return currencyNameResIds[code.uppercase(Locale.ROOT)]
}
