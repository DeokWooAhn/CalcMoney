package com.ahn.presentation.main

import com.ahn.presentation.R

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: Int,
    val selectedIcon: Int
) {
    data object Calculator : BottomNavItem(
        "calculator",
        "계산기",
        R.drawable.img_outline_calculate,
        R.drawable.img_calculator
    )

    data object Exchange : BottomNavItem(
        "exchange",
        "환율",
        R.drawable.img_exchange,
        R.drawable.img_outline_currency_exchange
    )

    data object Favorite :
        BottomNavItem(
            "favorite",
            "즐겨찾기",
            R.drawable.img_favorite_border,
            R.drawable.img_favorite
        )

    data object Settings :
        BottomNavItem(
            "settings",
            "설정",
            R.drawable.img_outline_settings,
            R.drawable.img_settings
        )
}