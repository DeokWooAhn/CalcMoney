package com.ahn.data.di

import com.ahn.data.repository.CalculatorHistoryRepositoryImpl
import com.ahn.data.repository.ExchangeRateRepositoryImpl
import com.ahn.data.repository.FavoriteCurrencyRepositoryImpl
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
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

    /**
     * Binds FavoriteCurrencyRepositoryImpl to FavoriteCurrencyRepository for Hilt injection.
     *
     * @return The FavoriteCurrencyRepository to be provided when the interface is requested.
     */
    @Binds
    abstract fun bindFavoriteCurrencyRepository(
        impl: FavoriteCurrencyRepositoryImpl
    ): FavoriteCurrencyRepository

    /**
     * Binds the concrete `CalculatorHistoryRepositoryImpl` to the `CalculatorHistoryRepository` interface for dependency injection.
     *
     * @param impl The repository implementation to provide when `CalculatorHistoryRepository` is requested.
     * @return The bound `CalculatorHistoryRepository` interface.
     */
    @Binds
    abstract fun bindCalculatorHistoryRepository(
        impl: CalculatorHistoryRepositoryImpl
    ): CalculatorHistoryRepository
}