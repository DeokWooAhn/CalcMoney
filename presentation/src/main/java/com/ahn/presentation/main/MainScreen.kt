package com.ahn.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahn.presentation.R

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController()
) {
    val statusBarPadding = WindowInsets
        .statusBars
        .asPaddingValues()
        .calculateTopPadding()

    val items = listOf(
        BottomNavItem.Calculator,
        BottomNavItem.Exchange,
        BottomNavItem.Favorite,
        BottomNavItem.Settings
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.black))
            .padding(top = statusBarPadding),
        bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(65.dp)) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { item ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == item.route } == true

                        NavigationBarItem(
                            modifier = Modifier.fillMaxHeight(),
                            icon = {
                                Icon(
                                    painterResource(if (selected) item.selectedIcon else item.icon),
                                    contentDescription = item.title
                                )
                            },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
        }
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        )
    }
}