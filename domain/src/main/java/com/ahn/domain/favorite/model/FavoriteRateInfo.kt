package com.ahn.domain.favorite.model

import com.ahn.domain.currency.model.CurrencyInfo

data class FavoriteRateInfo(
    val currency: CurrencyInfo,
    val baseCurrencyCode: String,
    val rate: Double,
    val convertedAmount: Double,
)
