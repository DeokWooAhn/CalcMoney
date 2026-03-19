package com.ahn.domain.usecase

import com.ahn.domain.repository.FavoriteCurrencyRepository
import javax.inject.Inject

class ToggleFavoriteCurrencyUseCase @Inject constructor(
    private val repository: FavoriteCurrencyRepository
) {
    suspend operator fun invoke(currencyCode: String) {
        if (repository.isFavorite(currencyCode)) {
            repository.removeFavorite(currencyCode)
        } else {
            repository.addFavorite(currencyCode)
        }
    }
}