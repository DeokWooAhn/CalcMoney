package com.ahn.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ahn.presentation.ui.screen.calculator.CalculatorRoute
import com.ahn.presentation.ui.screen.calculator.CalculatorScreen
import com.ahn.presentation.ui.screen.exchange.ExchangeScreen
import com.ahn.presentation.ui.screen.favorite.FavoriteScreen
import com.ahn.presentation.ui.screen.setting.SettingScreen

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Calculator.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Calculator.route) { CalculatorRoute() }
        composable(BottomNavItem.Exchange.route) { ExchangeScreen() }
        composable(BottomNavItem.Favorite.route) { FavoriteScreen() }
        composable(BottomNavItem.Settings.route) { SettingScreen() }
    }
}