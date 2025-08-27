package com.example.presentation.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.presentation.screen.calculator.CalculatorScreen
import com.example.presentation.screen.exchange.ExchangeScreen
import com.example.presentation.screen.favorite.FavoriteScreen
import com.example.presentation.screen.setting.SettingScreen

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Calculator.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Calculator.route) { CalculatorScreen() }
        composable(BottomNavItem.Exchange.route) { ExchangeScreen() }
        composable(BottomNavItem.Favorite.route) { FavoriteScreen() }
        composable(BottomNavItem.Settings.route) { SettingScreen() }
    }
}