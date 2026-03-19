package com.ahn.data.repository

import com.ahn.data.local.FavoriteCurrencyDataSource
import com.ahn.domain.repository.FavoriteCurrencyRepository
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

    override suspend fun isFavorite(currencyCode: String): Boolean {
        return dataSource.isFavorite(currencyCode)
    }


}