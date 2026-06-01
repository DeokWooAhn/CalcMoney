package com.ahn.domain.favorite.repository

import kotlinx.coroutines.flow.Flow

interface FavoriteCurrencyRepository {
    fun getFavoriteCurrencyCodes(): Flow<List<String>>

    suspend fun addFavorite(currencyCode: String)

    suspend fun removeFavorite(currencyCode: String)

    suspend fun isFavorite(currencyCode: String): Boolean
}
