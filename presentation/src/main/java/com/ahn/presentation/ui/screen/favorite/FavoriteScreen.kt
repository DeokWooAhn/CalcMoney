package com.ahn.presentation.ui.screen.favorite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.screen.exchange.ExchangeContract
import com.ahn.presentation.ui.screen.exchange.ExchangeInputContainer
import com.ahn.presentation.ui.screen.exchange.ExchangeViewModel
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun FavoriteRoute(
    exchangeViewModel: ExchangeViewModel,
    favoriteViewModel: FavoriteViewModel = hiltViewModel()
) {
    val exchangeState by exchangeViewModel.collectAsState()
    val favoriteState by favoriteViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    exchangeViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ExchangeContract.SideEffect.ShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbarImmediately(sideEffect.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        exchangeViewModel.processIntent(ExchangeContract.Intent.LoadCurrencies)
    }

    LaunchedEffect(
        exchangeState.fromCurrency,
        exchangeState.fromAmount,
        exchangeState.favoriteCurrencyCodes,
        exchangeState.availableCurrencies,
    ) {
        favoriteViewModel.onExchangeStateChanged(exchangeState)
    }

    FavoriteScreen(
        exchangeState = exchangeState,
        favoriteState = favoriteState,
        onExchangeIntent = exchangeViewModel::processIntent,
        snackbarHostState = snackbarHostState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    exchangeState: ExchangeContract.State,
    favoriteState: FavoriteContract.State,
    onExchangeIntent: (ExchangeContract.Intent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        snackbarHost = { CustomSnackbarHost(snackbarHostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "즐겨찾기",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ExchangeInputContainer(
                amount = exchangeState.fromAmount,
                currency = exchangeState.fromCurrency,
                onAmountChange = { onExchangeIntent(ExchangeContract.Intent.UpdateFromAmount(it)) },
                onCurrencyClick = { onExchangeIntent(ExchangeContract.Intent.SelectFromCurrency(it)) },
                availableCurrencies = exchangeState.availableCurrencies,
                favoriteCurrencyCodes = exchangeState.favoriteCurrencyCodes,
                onToggleFavorite = { onExchangeIntent(ExchangeContract.Intent.ToggleFavorite(it)) },
                isEditable = true,
                label = "기준 금액",
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                favoriteState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                exchangeState.favoriteCurrencyCodes.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "즐겨찾기한 통화가 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                favoriteState.items.isEmpty() -> {
                    Text(
                        text = "환율 정보를 불러올 수 없습니다.",
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(800.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = favoriteState.items,
                            key = { it.currency.code },
                        ) { item ->
                            FavoriteRateCard(
                                item = item,
                                onRemoveFavorite = {
                                    onExchangeIntent(ExchangeContract.Intent.ToggleFavorite(item.currency.code))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteRateCard(
    item: FavoriteContract.Item,
    onRemoveFavorite: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(item.currency.flagEmoji, fontSize = 22.sp)
                    Column {
                        Text(item.currency.code, fontWeight = FontWeight.SemiBold)
                        Text(
                            item.currency.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
                IconButton(onClick = onRemoveFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "즐겨찾기 해제",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(item.convertedAmount, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                item.rateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoriteScreenPreview() {
    FavoriteScreen(
        exchangeState = ExchangeContract.State(),
        favoriteState = FavoriteContract.State(),
        onExchangeIntent = {},
        snackbarHostState = remember { SnackbarHostState() }
    )
}