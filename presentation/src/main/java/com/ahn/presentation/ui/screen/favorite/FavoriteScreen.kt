package com.ahn.presentation.ui.screen.favorite

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.presentation.R
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.component.ExchangeInputContainer
import com.ahn.presentation.ui.screen.exchange.ExchangeContract
import com.ahn.presentation.ui.screen.exchange.ExchangeViewModel
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@Composable
fun FavoriteRoute(
    exchangeViewModel: ExchangeViewModel,
    favoriteViewModel: FavoriteViewModel = hiltViewModel(),
) {
    val exchangeState by exchangeViewModel.collectAsState()
    val favoriteState by favoriteViewModel.state.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    exchangeViewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ExchangeContract.SideEffect.ShowSnackBar -> {
                scope.launch {
                    snackBarHostState.showSnackbarImmediately(sideEffect.message.asString(context))
                }
            }
        }
    }

    LaunchedEffect(
        exchangeState.fromCurrency,
        exchangeState.favoriteCurrencyCodes,
        exchangeState.availableCurrencies,
    ) {
        favoriteViewModel.onExchangeStateChanged(
            fromCurrency = exchangeState.fromCurrency,
            favoriteCurrencyCodes = exchangeState.favoriteCurrencyCodes,
            availableCurrencies = exchangeState.availableCurrencies,
        )
    }

    LaunchedEffect(exchangeState.fromAmount) {
        favoriteViewModel.onBaseAmountChanged(exchangeState.fromAmount)
    }

    FavoriteScreen(
        exchangeState = exchangeState,
        favoriteState = favoriteState,
        onExchangeIntent = exchangeViewModel::processIntent,
        snackbarHostState = snackBarHostState,
    )
}

@Composable
fun FavoriteScreen(
    exchangeState: ExchangeContract.State,
    favoriteState: FavoriteContract.State,
    onExchangeIntent: (ExchangeContract.Intent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        snackbarHost = { CustomSnackbarHost(snackbarHostState = snackbarHostState) },
        topBar = { FavoriteTopBar() },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        FavoriteContent(
            exchangeState = exchangeState,
            favoriteState = favoriteState,
            onExchangeIntent = onExchangeIntent,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.favorite_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun FavoriteContent(
    exchangeState: ExchangeContract.State,
    favoriteState: FavoriteContract.State,
    onExchangeIntent: (ExchangeContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
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
            label = stringResource(R.string.base_amount),
        )

        Spacer(modifier = Modifier.height(16.dp))

        FavoriteRateContent(
            exchangeState = exchangeState,
            favoriteState = favoriteState,
            onExchangeIntent = onExchangeIntent,
        )
    }
}

@Composable
private fun ColumnScope.FavoriteRateContent(
    exchangeState: ExchangeContract.State,
    favoriteState: FavoriteContract.State,
    onExchangeIntent: (ExchangeContract.Intent) -> Unit,
) {
    when {
        favoriteState.isLoading -> FavoriteLoading()
        exchangeState.favoriteCurrencyCodes.isEmpty() -> FavoriteEmptyMessage(R.string.empty_favorite_currency)
        favoriteState.items.isEmpty() -> FavoriteEmptyMessage(R.string.favorite_rate_load_failed)
        else -> FavoriteRateGrid(
            items = favoriteState.items,
            onRemoveFavorite = { currencyCode ->
                onExchangeIntent(ExchangeContract.Intent.ToggleFavorite(currencyCode))
            },
        )
    }
}

@Composable
private fun ColumnScope.FavoriteLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ColumnScope.FavoriteEmptyMessage(
    messageResId: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageResId),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.FavoriteRateGrid(
    items: List<FavoriteContract.Item>,
    onRemoveFavorite: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = items,
            key = { it.currency.code },
        ) { item ->
            FavoriteRateCard(
                item = item,
                onRemoveFavorite = { onRemoveFavorite(item.currency.code) },
            )
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
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        item.currency.flagEmoji,
                        fontSize = 22.sp,
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            item.currency.code,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            item.currency.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                        )
                    }
                }
                IconButton(
                    onClick = onRemoveFavorite,
                    modifier = Modifier.requiredSize(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = stringResource(R.string.remove_favorite),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                item.convertedAmount,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
        snackbarHostState = remember { SnackbarHostState() },
    )
}

@Preview(showBackground = true)
@Composable
private fun FavoriteRateCardPreview() {
    MaterialTheme {
        FavoriteRateCard(
            item = FavoriteContract.Item(
                currency = com.ahn.domain.currency.model.CurrencyInfo(
                    code = "USD",
                    displayCode = "USD",
                    name = stringResource(R.string.preview_currency_usd),
                    flagEmoji = "🇺🇸",
                ),
                convertedAmount = "1,433.20",
                rateLabel = "1 KRW = 0.0007 USD",
            ),
            onRemoveFavorite = {},
        )
    }
}
