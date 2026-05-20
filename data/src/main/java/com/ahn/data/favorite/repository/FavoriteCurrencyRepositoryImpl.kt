package com.ahn.data.favorite.repository

import com.ahn.data.favorite.local.datasource.FavoriteCurrencyDataSource
import com.ahn.domain.favorite.repository.FavoriteCurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavoriteCurrencyRepositoryImpl @Inject constructor(
    private val dataSource: FavoriteCurrencyDataSource
) : FavoriteCurrencyRepository {

    override fun getFavoriteCurrencyCodes(): Flow<List<String>> {
        return dataSource.getFavoriteCodes()
    }

    override suspend fun addFavorite(currencyCode: String) {
        dataSource.addFavorite(currencyCode)
    }

    override suspend fun removeFavorite(currencyCode: String) {
        dataSource.removeFavorite(currencyCode)
    }

    /**
     * Checks whether the given currency code is marked as a favorite.
     *
     * @param currencyCode The currency code to check.
     * @return `true` if the currency code is marked as a favorite, `false` otherwise.
     */
    override suspend fun isFavorite(currencyCode: String): Boolean {
        return dataSource.isFavorite(currencyCode)
    }
}
