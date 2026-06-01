package com.ahn.presentation.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val ColorScheme.buttonNumber: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkButtonNumber else LightButtonNumber

val ColorScheme.buttonFunction: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkButtonFunction else LightButtonFunction

val ColorScheme.buttonOperator: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkButtonOperator else LightButtonOperator

val ColorScheme.calculatorAccent: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkButtonOperator else LightCalculatorAccent

val ColorScheme.currencySelectorSurface: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkCurrencySelectorSurface else LightCurrencySelectorSurface

val ColorScheme.currencySelectorBorder: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkCurrencySelectorBorder else LightCurrencySelectorBorder

val ColorScheme.buttonTextPrimary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkButtonTextPrimary else LightButtonTextPrimary

val ColorScheme.buttonTextSecondary: Color
    @Composable
    get() = if (isSystemInDarkTheme()) DarkButtonTextSecondary else LightButtonTextSecondary

private val DarkColorScheme = darkColorScheme(
    primary = Orange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceContainer = DarkBackground,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkGray,
)

private val LightColorScheme = lightColorScheme(
    primary = Orange,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceContainer = Color.White,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightGray,
)

@Composable
fun CalcMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
