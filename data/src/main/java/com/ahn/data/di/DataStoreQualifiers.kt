package com.ahn.data.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CalculatorHistoryPreferencesDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CurrencySelectionPreferencesDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FavoriteCurrencyPreferencesDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ThemePreferencesDataStore
