package com.ahn.presentation.main

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ahn.presentation.ui.screen.calculator.CalculatorRoute
import com.ahn.presentation.ui.screen.exchange.ExchangeRoute
import com.ahn.presentation.ui.screen.favorite.FavoriteScreen
import com.ahn.presentation.ui.screen.setting.SettingScreen

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Calculator.route,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(BottomNavItem.Calculator.route) { CalculatorRoute() }
        composable(BottomNavItem.Exchange.route) { ExchangeRoute() }
        composable(BottomNavItem.Favorite.route) { FavoriteScreen() }
        composable(BottomNavItem.Settings.route) { SettingScreen() }
    }
}