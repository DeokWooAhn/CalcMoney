package com.ahn.data.exchange.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ahn.data.exchange.local.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRateEntity>

    @Query("SELECT MAX(fetchedAt) FROM exchange_rates")
    suspend fun getLatestFetchedAt(): Long?

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExchangeRateEntity>)

    @Transaction
    suspend fun replaceAll(items: List<ExchangeRateEntity>) {
        deleteAll()
        insertAll(items)
    }
}