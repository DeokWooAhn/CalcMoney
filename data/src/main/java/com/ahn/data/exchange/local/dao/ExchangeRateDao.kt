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

    @Query("SELECT MAX(rateDate) FROM exchange_rates")
    suspend fun getLatestRateDate(): String?

    @Query("DELETE FROM exchange_rates")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ExchangeRateEntity>)

    /**
     * `exchange_rates` 테이블의 전체 내용을 하나의 트랜잭션으로 교체합니다.
     *
     * 기존 데이터를 모두 삭제한 뒤 새 데이터를 삽입하며, 중간에 실패하면 전체 변경이 적용되지 않습니다.
     */
    @Transaction
    suspend fun replaceAll(items: List<ExchangeRateEntity>) {
        deleteAll()
        insertAll(items)
    }
}
