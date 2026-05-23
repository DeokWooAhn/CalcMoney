package com.ahn.data.di

import com.ahn.data.calculator.repository.CalculatorHistoryRepositoryImpl
import com.ahn.data.currency.repository.CurrencySelectionRepositoryImpl
import com.ahn.data.exchange.repository.ExchangeRateRepositoryImpl
import com.ahn.data.favorite.repository.FavoriteCurrencyRepositoryImpl
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import com.ahn.domain.currency.repository.CurrencySelectionRepository
import com.ahn.domain.exchange.repository.ExchangeRateRepository
import com.ahn.domain.favorite.repository.FavoriteCurrencyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindExchangeRateRepository(
        impl: ExchangeRateRepositoryImpl
    ): ExchangeRateRepository

    @Binds
    abstract fun bindFavoriteCurrencyRepository(
        impl: FavoriteCurrencyRepositoryImpl
    ): FavoriteCurrencyRepository

    @Binds
    abstract fun bindCalculatorHistoryRepository(
        impl: CalculatorHistoryRepositoryImpl
    ): CalculatorHistoryRepository

    @Binds
    abstract fun bindCurrencySelectionRepository(
        impl: CurrencySelectionRepositoryImpl
    ): CurrencySelectionRepository

}
