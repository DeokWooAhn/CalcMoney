package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import com.ahn.domain.currency.model.CurrencyInfo

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
        name = "Korean Won",
        flagEmoji = getFlagEmoji("KRW"),
    )
}

private fun String.toRateNumber(currencyUnit: String? = null): Double? {
    val number = replace(",", "").toDoubleOrNull() ?: return null
    return if (currencyUnit?.contains("(100)") == true) {
        number / 100.0
    } else {
        number
    }
}

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

private fun countryFlag(countryCode: String): String {
    return countryCode.uppercase().map { char ->
        Character.toChars(0x1F1E6 + (char.code - 'A'.code)).concatToString()
    }.joinToString("")
}
