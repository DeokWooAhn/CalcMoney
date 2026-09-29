package com.ahn.presentation.ui.screen.favorite

import com.ahn.domain.currency.model.CurrencyInfo

object FavoriteContract {
    data class Item(
        val currency: CurrencyInfo,
        val convertedAmount: String,
        val rateLabel: String,
    )

    data class State(
        val isLoading: Boolean = false,
        val items: List<Item> = emptyList(),
        /**
         * 즐겨찾기가 현재 기준 통화 하나뿐일 때의 기준 통화 코드.
         * 기준 통화는 카드로 만들지 않으므로 목록이 비는데, 이를 로드 실패와 구분하는 데 쓴다.
         */
        val baseOnlyFavoriteCode: String? = null,
    )
}
