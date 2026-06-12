package com.ahn.presentation.main

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ahn.presentation.R
import com.ahn.presentation.ui.theme.CalcMoneyTheme
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahn.presentation.ads.AdConsentState

@Composable
fun MainScreen(
    navController: NavHostController = rememberNavController(),
    adConsentState: AdConsentState = AdConsentState(),
    onPrivacyOptionsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val backPressExitMessage = stringResource(R.string.back_press_exit)

    val statusBarPadding = WindowInsets
        .statusBars
        .asPaddingValues()
        .calculateTopPadding()

    val items = listOf(
        BottomNavItem.Calculator,
        BottomNavItem.Exchange,
        BottomNavItem.Favorite,
        BottomNavItem.Settings,
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isTopLevelDestination = items.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }

    var lastBackPressedTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = isTopLevelDestination) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastBackPressedTime < 2000L) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressedTime = currentTime
            Toast.makeText(context, backPressExitMessage, Toast.LENGTH_SHORT).show()
        }
    }

    MainScreenScaffold(
        items = items,
        selectedRoute = currentDestination?.route,
        statusBarPadding = statusBarPadding,
        onItemClick = { item ->
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { innerPadding ->
        MainNavGraph(
            navController = navController,
            adConsentState = adConsentState,
            onPrivacyOptionsClick = onPrivacyOptionsClick,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        )
    }
}

@Composable
private fun MainScreenScaffold(
    items: List<BottomNavItem>,
    selectedRoute: String?,
    statusBarPadding: androidx.compose.ui.unit.Dp,
    onItemClick: (BottomNavItem) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = statusBarPadding),
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = selectedRoute == item.route

                    NavigationBarItem(
                        icon = {
                            Icon(
                                painterResource(if (selected) item.selectedIcon else item.icon),
                                contentDescription = stringResource(item.titleRes),
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        selected = selected,
                        onClick = { onItemClick(item) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    CalcMoneyTheme {
        Surface {
            MainScreenScaffold(
                items = listOf(
                    BottomNavItem.Calculator,
                    BottomNavItem.Exchange,
                    BottomNavItem.Favorite,
                    BottomNavItem.Settings,
                ),
                selectedRoute = BottomNavItem.Calculator.route,
                statusBarPadding = 0.dp,
                onItemClick = { },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = stringResource(R.string.bottom_nav_calculator))
                }
            }
        }
    }
}
