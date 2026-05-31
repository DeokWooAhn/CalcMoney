package com.ahn.data.exchange.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ahn.data.common.database.CalcMoneyDatabase
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExchangeRateDaoTest {
    private lateinit var database: CalcMoneyDatabase
    private lateinit var dao: ExchangeRateDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(
            context,
            CalcMoneyDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = database.exchangeRateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndReadExchangeRates() = runBlocking {
        dao.insertAll(
            listOf(
                exchangeRate(code = "USD", baseRate = 1300.0, fetchedAt = 1000L),
                exchangeRate(code = "JPY", baseRate = 9.0, fetchedAt = 1000L),
            ),
        )

        val ratesByCode = dao.getAll().associateBy { it.code }

        assertEquals(2, ratesByCode.size)
        assertEquals(1300.0, ratesByCode.getValue("USD").baseRate, 0.0)
        assertEquals(9.0, ratesByCode.getValue("JPY").baseRate, 0.0)
    }

    @Test
    fun replaceRateWhenSameCurrencyCodeInserted() = runBlocking {
        dao.insertAll(listOf(exchangeRate(code = "USD", baseRate = 1300.0, fetchedAt = 1000L)))
        dao.insertAll(listOf(exchangeRate(code = "USD", baseRate = 1400.0, fetchedAt = 2000L)))

        val rates = dao.getAll()

        assertEquals(1, rates.size)
        assertEquals(1400.0, rates.single().baseRate, 0.0)
        assertEquals(2000L, rates.single().fetchedAt)
    }

    @Test
    fun returnLatestFetchedAt() = runBlocking {
        dao.insertAll(
            listOf(
                exchangeRate(code = "USD", fetchedAt = 1000L),
                exchangeRate(code = "JPY", fetchedAt = 3000L),
                exchangeRate(code = "EUR", fetchedAt = 2000L),
            ),
        )

        assertEquals(3000L, dao.getLatestFetchedAt())
    }

    @Test
    fun returnLatestRateDate() = runBlocking {
        dao.insertAll(
            listOf(
                exchangeRate(code = "USD", rateDate = "20260527"),
                exchangeRate(code = "JPY", rateDate = "20260529"),
                exchangeRate(code = "EUR", rateDate = "20260528"),
            ),
        )

        assertEquals("20260529", dao.getLatestRateDate())
    }

    @Test
    fun replaceAllDeletesOldRatesAndInsertsNewRates() = runBlocking {
        dao.insertAll(
            listOf(
                exchangeRate(code = "USD"),
                exchangeRate(code = "JPY"),
            ),
        )

        dao.replaceAll(listOf(exchangeRate(code = "EUR")))

        val rates = dao.getAll()

        assertEquals(1, rates.size)
        assertEquals("EUR", rates.single().code)
    }

    private fun exchangeRate(
        code: String,
        baseRate: Double = 1000.0,
        fetchedAt: Long = 1000L,
        rateDate: String = "20260529",
    ): ExchangeRateEntity {
        return ExchangeRateEntity(
            code = code,
            currencyUnit = code,
            currencyName = "$code currency",
            baseRate = baseRate,
            fetchedAt = fetchedAt,
            rateDate = rateDate,
        )
    }
}
