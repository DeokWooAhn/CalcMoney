package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import com.ahn.domain.currency.model.CurrencyInfo

/**
 * Convert this remote ExchangeRateResponse into an ExchangeRateEntity when the response is valid.
 *
 * The response is considered invalid and will yield `null` if `result` is not 1, if `currencyUnit`
 * or `baseRate` is null or blank, or if `baseRate` cannot be parsed into a numeric rate.
 *
 * @param fetchedAt Timestamp when the rates were fetched, in milliseconds since the epoch.
 * @return The mapped [ExchangeRateEntity] with normalized `code`, trimmed `currencyUnit`, resolved
 * currency name (falls back to `"Unknown"`), parsed `baseRate`, and the provided `fetchedAt`,
 * or `null` if the response is invalid or parsing fails.
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
 * Get the base exchange rate for the given currency code from this list.
 *
 * @param code ISO-like currency code (e.g., "USD"); `KRW` is treated as 1.0.
 * @return The currency's base rate if present, or `null` if not found.
 */
internal fun List<ExchangeRateEntity>.rateOf(code: String): Double? {
    if (code == "KRW") return 1.0
    return firstOrNull { it.code == code }?.baseRate
}

/**
 * Create a CurrencyInfo representation from this ExchangeRateEntity.
 *
 * @return A CurrencyInfo whose `code` and `displayCode` are the entity's `code`, `name` is the entity's `currencyName`, and `flagEmoji` is derived from the entity's code.
 */
internal fun ExchangeRateEntity.toCurrencyInfo(): CurrencyInfo {
    return CurrencyInfo(
        code = code,
        displayCode = code,
        name = currencyName,
        flagEmoji = getFlagEmoji(code),
    )
}

/**
 * Creates a CurrencyInfo representing the Korean Won.
 *
 * @return A CurrencyInfo with `code` and `displayCode` set to "KRW", `name` set to "Korean Won", and `flagEmoji` set to the Korea flag emoji.
 */
internal fun krwCurrencyInfo(): CurrencyInfo {
    return CurrencyInfo(
        code = "KRW",
        displayCode = "KRW",
        name = "Korean Won",
        flagEmoji = getFlagEmoji("KRW"),
    )
}

/**
 * Parses this numeric string into a rate and applies unit scaling when applicable.
 *
 * If `currencyUnit` contains the substring `"(100)"`, the parsed number is divided by 100.
 *
 * @param currencyUnit Optional currency unit string used to detect `(100)` scaling.
 * @return The parsed rate as a `Double`, or `null` if the string cannot be parsed as a number.
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
 * Map a three-letter currency code to the corresponding country flag emoji.
 *
 * @param currencyCode ISO 4217-like three-letter currency code (e.g., "USD", "KRW").
 * @return The flag emoji for the currency's associated country, or an empty string if the currency is not supported.
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
 * Convert an ISO-like country code into the corresponding regional-indicator flag emoji.
 *
 * @param countryCode A country code (typically two letters); matching is case-insensitive.
 * @return A string of regional-indicator symbols forming the flag emoji for the given code (empty string if `countryCode` is empty).
 */
private fun countryFlag(countryCode: String): String {
    return countryCode.uppercase().map { char ->
        Character.toChars(0x1F1E6 + (char.code - 'A'.code)).concatToString()
    }.joinToString("")
}
