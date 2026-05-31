package com.ahn.data.common.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ahn.data.exchange.local.dao.ExchangeRateDao
import com.ahn.data.exchange.local.entity.ExchangeRateEntity

@Database(
    entities = [ExchangeRateEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class CalcMoneyDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE exchange_rates ADD COLUMN rateDate TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
