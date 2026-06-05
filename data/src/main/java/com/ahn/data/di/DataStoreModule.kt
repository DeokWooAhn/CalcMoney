package com.ahn.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    @CalculatorHistoryPreferencesDataStore
    fun provideCalculatorHistoryDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return createPreferencesDataStore(context, "calculator_history")
    }

    @Provides
    @Singleton
    @CurrencySelectionPreferencesDataStore
    fun provideCurrencySelectionDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return createPreferencesDataStore(context, "currency_selection")
    }

    @Provides
    @Singleton
    @FavoriteCurrencyPreferencesDataStore
    fun provideFavoriteCurrencyDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return createPreferencesDataStore(context, "favorite_currencies")
    }

    @Provides
    @Singleton
    @ThemePreferencesDataStore
    fun provideThemePreferenceDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return createPreferencesDataStore(context, "theme_preferences")
    }

    private fun createPreferencesDataStore(
        context: Context,
        name: String,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(name) },
        )
    }
}
