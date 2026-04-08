package com.ahn.presentation.main

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ahn.presentation.ui.screen.calculator.CalculatorRoute
import com.ahn.presentation.ui.screen.exchange.ExchangeRoute
import com.ahn.presentation.ui.screen.favorite.FavoriteRoute
import com.ahn.presentation.ui.screen.setting.SettingScreen

@Composable
fun MainNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    val sharedOwner = LocalActivity.current as ViewModelStoreOwner

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
        composable(BottomNavItem.Exchange.route) {
            ExchangeRoute(viewModel = hiltViewModel(sharedOwner))
        }
        composable(BottomNavItem.Favorite.route) {
            FavoriteRoute(exchangeViewModel = hiltViewModel(sharedOwner))
        }
        composable(BottomNavItem.Settings.route) { SettingScreen() }
    }
}