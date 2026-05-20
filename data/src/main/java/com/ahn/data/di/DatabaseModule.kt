package com.ahn.data.di

import android.content.Context
import androidx.room.Room
import com.ahn.data.common.database.CalcMoneyDatabase
import com.ahn.data.exchange.local.dao.ExchangeRateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Provides the application's CalcMoneyDatabase instance.
     *
     * @param context The application [Context] used to construct the Room database.
     * @return A configured [CalcMoneyDatabase] backed by the "calc_money.db" file.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): CalcMoneyDatabase {
        return Room.databaseBuilder(
            context,
            CalcMoneyDatabase::class.java,
            "calc_money.db",
        ).build()
    }

    /**
     * Exposes the ExchangeRateDao from the application's CalcMoneyDatabase.
     *
     * @param database The CalcMoneyDatabase instance to retrieve the DAO from.
     * @return The ExchangeRateDao used to access exchange rate entities.
     */
    @Provides
    @Singleton
    fun provideExchangeRateDao(
        database: CalcMoneyDatabase,
    ): ExchangeRateDao = database.exchangeRateDao()
}
