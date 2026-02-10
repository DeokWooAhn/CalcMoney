package com.ahn.presentation.ui.screen.exchange

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ExchangeRoute(
    viewModel: ExchangeViewModel = hiltViewModel()
) {
    ExchangeScreen()
}

@Composable
fun ExchangeScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "교환 화면")
    }
}