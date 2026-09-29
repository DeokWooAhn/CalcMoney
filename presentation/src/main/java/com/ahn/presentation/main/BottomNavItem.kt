package com.ahn.presentation.main

import androidx.annotation.StringRes
import com.ahn.presentation.R

sealed class BottomNavItem(
    val route: String,
    @param:StringRes
    val titleRes: Int,
    val icon: Int,
    val selectedIcon: Int,
    /** E2E 테스트(Maestro)가 탭을 찾을 때 쓰는 id. iOS `MainTab.testID`와 값이 같아야 한다. */
    val testTag: String,
) {
    data object Calculator : BottomNavItem(
        Route.CALCULATOR,
        R.string.bottom_nav_calculator,
        R.drawable.img_outline_calculate,
        R.drawable.img_calculator,
        "tab.calculator",
    )

    data object Exchange : BottomNavItem(
        Route.EXCHANGE,
        R.string.bottom_nav_exchange,
        R.drawable.img_exchange,
        R.drawable.img_outline_currency_exchange,
        "tab.exchange",
    )

    data object Favorite :
        BottomNavItem(
            Route.FAVORITE,
            R.string.bottom_nav_favorite,
            R.drawable.img_favorite_border,
            R.drawable.img_favorite,
            "tab.favorite",
        )

    data object Settings :
        BottomNavItem(
            Route.SETTINGS,
            R.string.bottom_nav_settings,
            R.drawable.img_outline_settings,
            R.drawable.img_settings,
            "tab.setting",
        )
}
