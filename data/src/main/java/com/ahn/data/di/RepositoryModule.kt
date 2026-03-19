package com.ahn.data.di

import com.ahn.data.repository.ExchangeRateRepositoryImpl
import com.ahn.data.repository.FavoriteCurrencyRepositoryImpl
import com.ahn.domain.repository.ExchangeRateRepository
import com.ahn.domain.repository.FavoriteCurrencyRepository
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
}