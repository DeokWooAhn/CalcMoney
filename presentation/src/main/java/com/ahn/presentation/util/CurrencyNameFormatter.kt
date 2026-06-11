package com.ahn.presentation.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R

@Composable
fun CurrencyInfo.localizedName(): String {
    return currencyNameResId(code)?.let { stringResource(it) } ?: name
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
    return currencyNameResIds[code.uppercase()]
}
