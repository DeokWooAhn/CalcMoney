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
     * 환율 캐시 DB를 만듭니다.
     *
     * 마이그레이션 경로가 없으면 앱이 시작하자마자 크래시하므로, 그때는 테이블을 비우고 새로 만듭니다.
     * 이 DB에는 Firestore에서 다시 받으면 되는 환율 캐시만 있어 지워도 됩니다.
     * 사용자 데이터를 여기에 두게 되면 이 대체 동작을 다시 검토해야 합니다.
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): CalcMoneyDatabase {
        return Room
            .databaseBuilder(
                context,
                CalcMoneyDatabase::class.java,
                "calc_money.db",
            ).addMigrations(CalcMoneyDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideExchangeRateDao(
        database: CalcMoneyDatabase,
    ): ExchangeRateDao = database.exchangeRateDao()
}
