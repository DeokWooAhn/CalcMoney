package com.ahn.presentation.main

import androidx.annotation.StringRes
import com.ahn.presentation.R

sealed class BottomNavItem(
    val route: String,
    @param:StringRes
    val titleRes: Int,
    val icon: Int,
    val selectedIcon: Int
) {
    data object Calculator : BottomNavItem(
        Route.CALCULATOR,
        R.string.bottom_nav_calculator,
        R.drawable.img_outline_calculate,
        R.drawable.img_calculator
    )

    data object Exchange : BottomNavItem(
        Route.EXCHANGE,
        R.string.bottom_nav_exchange,
        R.drawable.img_exchange,
        R.drawable.img_outline_currency_exchange
    )

    data object Favorite :
        BottomNavItem(
            Route.FAVORITE,
            R.string.bottom_nav_favorite,
            R.drawable.img_favorite_border,
            R.drawable.img_favorite
        )

    data object Settings :
        BottomNavItem(
            Route.SETTINGS,
            R.string.bottom_nav_settings,
            R.drawable.img_outline_settings,
            R.drawable.img_settings
        )
}
