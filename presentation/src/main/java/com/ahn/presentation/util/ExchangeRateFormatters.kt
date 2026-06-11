package com.ahn.presentation.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatExchangeRateDate(rateDate: String): String {
    return if (rateDate.length == 8) {
        "${rateDate.substring(0, 4)}.${rateDate.substring(4, 6)}.${rateDate.substring(6, 8)}"
    } else {
        rateDate
    }
}

fun formatExchangeRateFetchedAt(fetchedAt: Long): String? {
    if (fetchedAt <= 0L) return null

    return Instant.ofEpochMilli(fetchedAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA))
}
