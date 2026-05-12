package com.ahn.domain.favorite.usecase

import com.ahn.domain.favorite.repository.FavoriteCurrencyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoriteCurrenciesUseCase @Inject constructor(
    private val repository: FavoriteCurrencyRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return repository.getFavoriteCurrencyCodes()
    }
}