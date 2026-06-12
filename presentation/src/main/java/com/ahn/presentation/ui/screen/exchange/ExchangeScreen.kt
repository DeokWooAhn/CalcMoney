package com.ahn.presentation.ui.screen.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import com.ahn.presentation.ui.component.AdMobBanner
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.ui.component.ExchangeInputContainer
import com.ahn.presentation.util.formatExchangeRateDate
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.launch
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect
import java.util.Locale

@Composable
fun ExchangeRoute(
    viewModel: ExchangeViewModel = hiltViewModel(),
    canRequestAds: Boolean = false,
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is ExchangeContract.SideEffect.ShowSnackBar -> {
                scope.launch {
                    snackbarHostState.showSnackbarImmediately(sideEffect.message.asString(context))
                }
            }
        }
    }

    ExchangeScreen(
        state = state,
        canRequestAds = canRequestAds,
        onIntent = viewModel::processIntent,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun ExchangeScreen(
    state: ExchangeContract.State,
    canRequestAds: Boolean = false,
    onIntent: (ExchangeContract.Intent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val exchangeRateDateText = remember(state.exchangeRateDate) {
        state.exchangeRateDate.takeIf { it.isNotBlank() }?.let(::formatExchangeRateDate)
    }

    Scaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = snackbarHostState)
        },
        topBar = { ExchangeTopBar() },
        bottomBar = {
            AdMobBanner(
                adUnitIdResId = R.string.admob_exchange_banner_id,
                canRequestAds = canRequestAds,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        ExchangeContent(
            state = state,
            exchangeRateDateText = exchangeRateDateText,
            onIntent = onIntent,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExchangeTopBar() {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.exchange_title),
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
private fun ExchangeContent(
    state: ExchangeContract.State,
    exchangeRateDateText: String?,
    onIntent: (ExchangeContract.Intent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExchangeInputContainer(
            amount = state.fromAmount,
            currency = state.fromCurrency,
            onAmountChange = { onIntent(ExchangeContract.Intent.UpdateFromAmount(it)) },
            onCurrencyClick = { onIntent(ExchangeContract.Intent.SelectFromCurrency(it)) },
            availableCurrencies = state.availableCurrencies,
            favoriteCurrencyCodes = state.favoriteCurrencyCodes,
            onToggleFavorite = { onIntent(ExchangeContract.Intent.ToggleFavorite(it)) },
            isEditable = true,
            label = stringResource(R.string.base_amount),
        )

        Spacer(modifier = Modifier.height(16.dp))

        SwapCurrencyButton(
            onClick = { onIntent(ExchangeContract.Intent.SwapCurrencies) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExchangeInputContainer(
            amount = state.toAmount,
            currency = state.toCurrency,
            onAmountChange = { },
            onCurrencyClick = { onIntent(ExchangeContract.Intent.SelectToCurrency(it)) },
            availableCurrencies = state.availableCurrencies,
            favoriteCurrencyCodes = state.favoriteCurrencyCodes,
            onToggleFavorite = { onIntent(ExchangeContract.Intent.ToggleFavorite(it)) },
            isEditable = false,
            label = stringResource(R.string.target_amount),
        )

        ExchangeRateInfo(
            fromCurrency = state.fromCurrency,
            toCurrency = state.toCurrency,
            exchangeRate = state.exchangeRate,
            exchangeRateDateText = exchangeRateDateText,
        )
    }
}

@Composable
private fun SwapCurrencyButton(
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = stringResource(R.string.swap_currency),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun ExchangeRateInfo(
    fromCurrency: CurrencyInfo?,
    toCurrency: CurrencyInfo?,
    exchangeRate: Double,
    exchangeRateDateText: String?,
) {
    if (fromCurrency == null || toCurrency == null || exchangeRate <= 0) return

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = "1 ${fromCurrency.code} = ${
            String.format(
                Locale.US,
                "%.4f",
                exchangeRate,
            )
        } ${toCurrency.code}",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    exchangeRateDateText?.let { rateDate ->
        Text(
            text = stringResource(R.string.exchange_rate_date, rateDate),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeScreenPreview() {
    ExchangeScreen(
        state = ExchangeContract.State(
            fromAmount = "1",
            toAmount = "1350.0000",
            fromCurrency = CurrencyInfo(
                code = "USD",
                displayCode = "USD",
                name = stringResource(R.string.preview_currency_usd),
                flagEmoji = "🇺🇸",
            ),
            toCurrency = CurrencyInfo(
                code = "KRW",
                displayCode = "KRW",
                name = stringResource(R.string.preview_currency_krw),
                flagEmoji = "🇰🇷",
            ),
            availableCurrencies = listOf(
                CurrencyInfo(
                    code = "USD",
                    displayCode = "USD",
                    name = stringResource(R.string.preview_currency_usd),
                    flagEmoji = "🇺🇸",
                ),
                CurrencyInfo(
                    code = "KRW",
                    displayCode = "KRW",
                    name = stringResource(R.string.preview_currency_krw),
                    flagEmoji = "🇰🇷",
                ),
            ),
            exchangeRate = 1350.0,
            exchangeRateDate = "20260529",
        ),
        onIntent = {},
    )
}
