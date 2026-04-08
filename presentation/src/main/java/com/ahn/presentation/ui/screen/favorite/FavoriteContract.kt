package com.ahn.presentation.ui.screen.favorite

import com.ahn.domain.model.CurrencyInfo

object FavoriteContract {
    data class Item(
        val currency: CurrencyInfo,
        val convertedAmount: String,
        val rateLabel: String,
    )

    data class State(
        val isLoading: Boolean = false,
        val items: List<Item> = emptyList(),
    )
}