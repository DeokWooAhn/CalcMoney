package com.ahn.domain.model

data class FavoriteRateInfo(
    val currency: CurrencyInfo,
    val baseCurrencyCode: String,
    val rate: Double,
    val convertedAmount: Double,
)
