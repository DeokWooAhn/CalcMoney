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
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): CalcMoneyDatabase {
        return Room.databaseBuilder(
            context,
            CalcMoneyDatabase::class.java,
            "calc_money.db",
        )
            .addMigrations(CalcMoneyDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideExchangeRateDao(
        database: CalcMoneyDatabase,
    ): ExchangeRateDao = database.exchangeRateDao()
}
