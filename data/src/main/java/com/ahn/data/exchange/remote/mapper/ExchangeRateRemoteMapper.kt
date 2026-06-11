package com.ahn.data.exchange.remote.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity

internal fun Any?.toExchangeRateEntities(
    fetchedAt: Long,
    rateDate: String,
): List<ExchangeRateEntity> {
    val rates = this as? List<*> ?: return emptyList()

    return rates.mapNotNull { it.toExchangeRateEntity(fetchedAt, rateDate) }
}

private fun Any?.toExchangeRateEntity(
    fetchedAt: Long,
    rateDate: String,
): ExchangeRateEntity? {
    val rate = this as? Map<*, *> ?: return null
    val code = rate["code"] as? String ?: return null
    val baseRate = rate["baseRate"].toDoubleOrNull() ?: return null

    return ExchangeRateEntity(
        code = code,
        currencyUnit = rate["currencyUnit"] as? String ?: code,
        currencyName = rate["currencyName"] as? String ?: "Unknown",
        baseRate = baseRate,
        fetchedAt = fetchedAt,
        rateDate = rateDate,
    )
}

private fun Any?.toDoubleOrNull(): Double? {
    return when (this) {
        is Number -> toDouble()
        is String -> runCatching { replace(",", "").toDouble() }.getOrNull()
        else -> null
    }
}
