package com.ahn.domain.favorite.usecase

import javax.inject.Inject

class FavoriteUseCases @Inject constructor(
    val buildFavoriteRates: BuildFavoriteRatesUseCase,
    val getFavoriteCurrencies: GetFavoriteCurrenciesUseCase,
    val toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase,
)
