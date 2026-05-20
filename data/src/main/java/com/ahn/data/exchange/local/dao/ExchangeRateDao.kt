package com.ahn.data.exchange.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ahn.data.exchange.local.entity.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    /**
     * Retrieve all rows from the `exchange_rates` table.
     *
     * @return A list of `ExchangeRateEntity` objects representing every row in the table; an empty list if the table has no rows.
     */
    @Query("SELECT * FROM exchange_rates")
    suspend fun getAll(): List<ExchangeRateEntity>

    /**
     * Retrieves the most recent `fetchedAt` timestamp from the `exchange_rates` table.
     *
     * @return The largest `fetchedAt` value, or `null` if the table contains no rows.
     */
    @Query("SELECT MAX(fetchedAt) FROM exchange_rates")
    suspend fun getLatestFetchedAt(): Long?

    /**
     * Deletes all rows from the `exchange_rates` table.
     */
    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    /**
     * Inserts the given exchange rate entities into the database, replacing existing rows on key conflicts.
     *
     * @param items The list of ExchangeRateEntity objects to insert into the `exchange_rates` table. Existing rows with the same primary or unique keys will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExchangeRateEntity>)

    /**
     * Replace the entire `exchange_rates` table contents with the provided list in a single transaction.
     *
     * This operation is atomic: either all existing rows are removed and the new `items` are inserted, or no change is applied.
     *
     * @param items The list of `ExchangeRateEntity` objects to become the new table contents.
     */
    @Transaction
    suspend fun replaceAll(items: List<ExchangeRateEntity>) {
        deleteAll()
        insertAll(items)
    }
}