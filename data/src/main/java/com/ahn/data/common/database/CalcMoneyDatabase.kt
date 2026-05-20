package com.ahn.data.common.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ahn.data.exchange.local.dao.ExchangeRateDao
import com.ahn.data.exchange.local.entity.ExchangeRateEntity

@Database(
    entities = [ExchangeRateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class CalcMoneyDatabase : RoomDatabase() {
    /**
 * Provides the DAO for accessing and manipulating exchange rate records.
 *
 * @return The [ExchangeRateDao] used to query and update exchange rate entities.
 */
abstract fun exchangeRateDao(): ExchangeRateDao
}
