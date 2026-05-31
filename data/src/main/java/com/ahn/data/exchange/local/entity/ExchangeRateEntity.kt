package com.ahn.data.exchange.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey val code: String,
    val currencyUnit: String,
    val currencyName: String,
    val baseRate: Double,
    val fetchedAt: Long,
    val rateDate: String,
)
