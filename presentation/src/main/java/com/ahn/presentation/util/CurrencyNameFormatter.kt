package com.ahn.presentation.util

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import java.util.Currency
import java.util.Locale

/**
 * 화면 언어에 맞는 통화 이름을 반환합니다.
 *
 * 직접 다듬은 번역이 있는 통화는 문자열 리소스를, 그 밖의 통화는 플랫폼이 제공하는 현지화 이름을 씁니다.
 * 플랫폼이 모르는 통화(ISO 4217 에 없는 지역 통화, 기기 OS 가 아직 모르는 최근 통화)는 예비 이름을 씁니다.
 * 예비 이름도 없으면 서버가 준 이름을 쓰는데, 서버 이름은 한국어라 다른 언어 기기에서는 통화 코드를 씁니다.
 */
@Composable
fun CurrencyInfo.localizedName(): String = localizedName(LocalResources.current, LocalConfiguration.current.locales[0])

/**
 * [localizedName]과 같은 규칙으로 이름을 만듭니다. Composable 밖에서 통화 목록 전체의 이름을 한 번에 만들 때 씁니다.
 *
 * 직접 다듬은 번역은 한국어·영어에만 있습니다. 다른 언어 기기에서 이 번역을 쓰면 다듬은 통화만 영어로, 나머지는
 * 기기 언어로 나와 목록 안에서 언어가 섞입니다. 그래서 그 밖의 언어에서는 모든 통화에 플랫폼 이름을 씁니다.
 */
internal fun CurrencyInfo.localizedName(resources: Resources, locale: Locale): String {
    val resId = currencyNameResId(code)
    if (resId != null && locale.language in curatedNameLanguages) return resources.getString(resId)

    return platformCurrencyName(code, locale)
        ?: resId?.let(resources::getString)
        ?: fallbackCurrencyNameResIds[code.uppercase(Locale.ROOT)]?.let(resources::getString)
        ?: serverNameOrCode(locale)
}

/** 직접 다듬은 통화 이름이 있는 언어입니다 (`values`, `values-ko`). */
private val curatedNameLanguages = setOf("ko", "en")

/** 서버가 준 통화 이름의 언어입니다. */
private const val SERVER_NAME_LANGUAGE = "ko"

/** 서버 이름은 한국어라 한국어 기기에서만 씁니다. 다른 언어에서는 목록에 한국어가 섞이지 않게 통화 코드를 씁니다. */
private fun CurrencyInfo.serverNameOrCode(locale: Locale): String =
    if (locale.language == SERVER_NAME_LANGUAGE) name else displayCode

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

/**
 * 플랫폼이 이름을 모를 수 있는 통화의 예비 이름입니다 (iOS `fallbackCurrencyNameKeys` 대응).
 *
 * ISO 4217 에 없는 지역 통화와, 최소 지원 버전(Android 9)의 ICU 가 아직 모르는 최근 통화입니다.
 * 플랫폼 이름이 있으면 그쪽을 먼저 씁니다. 한국어는 서버의 이름과 같습니다.
 */
private val fallbackCurrencyNameResIds = mapOf(
    "FOK" to R.string.currency_name_fok,
    "GGP" to R.string.currency_name_ggp,
    "IMP" to R.string.currency_name_imp,
    "JEP" to R.string.currency_name_jep,
    "KID" to R.string.currency_name_kid,
    "TVD" to R.string.currency_name_tvd,
    "MRU" to R.string.currency_name_mru,
    "SLE" to R.string.currency_name_sle,
    "STN" to R.string.currency_name_stn,
    "VES" to R.string.currency_name_ves,
    "XCG" to R.string.currency_name_xcg,
    "ZWG" to R.string.currency_name_zwg,
)

@StringRes
private fun currencyNameResId(code: String): Int? {
    return currencyNameResIds[code.uppercase(Locale.ROOT)]
}
