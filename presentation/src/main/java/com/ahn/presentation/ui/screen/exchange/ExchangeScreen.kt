package com.ahn.presentation.ui.screen.exchange

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahn.presentation.ui.component.CurrencySelector
import com.ahn.presentation.ui.component.CustomSnackbarHost
import com.ahn.presentation.util.showSnackbarImmediately
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ExchangeRoute(
    viewModel: ExchangeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is ExchangeContract.SideEffect.ShowSnackBar -> {
                    scope.launch {
                        snackbarHostState.showSnackbarImmediately(sideEffect.message)
                    }
                }
            }
        }
    }

    ExchangeScreen(
        state = state,
        onIntent = viewModel::processIntent,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeScreen(
    state: ExchangeContract.State,
    onIntent: (ExchangeContract.Intent) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        snackbarHost = {
            CustomSnackbarHost(snackbarHostState = snackbarHostState)
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "환율",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 10.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            ExchangeInputContainer(
                amount = state.fromAmount,
                currency = state.fromCurrency,
                onAmountChange = { onIntent(ExchangeContract.Intent.UpdateFromAmount(it)) },
                onCurrencyClick = { onIntent(ExchangeContract.Intent.SelectFromCurrency(it)) },
                isEditable = true,
                label = "기준 금액",
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { onIntent(ExchangeContract.Intent.SwapCurrencies) }) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "통화 교환",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ExchangeInputContainer(
                amount = state.toAmount,
                currency = state.toCurrency,
                onAmountChange = { },
                onCurrencyClick = { onIntent(ExchangeContract.Intent.SelectToCurrency(it)) },
                isEditable = false,
                label = "받을 금액",
            )

            if (state.exchangeRate > 0) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "1 ${state.fromCurrency.code} = ${
                        String.format(
                            Locale.US,
                            "%.4f",
                            state.exchangeRate
                        )
                    } ${state.toCurrency.code}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExchangeInputContainer(
    amount: String,
    currency: Currency,
    onAmountChange: (String) -> Unit,
    onCurrencyClick: (Currency) -> Unit,
    isEditable: Boolean,
    label: String,
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(16.dp)
                )
                .padding(15.dp, 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditable) {
                BasicTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            } else {
                Text(
                    text = amount.ifEmpty { "0" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            CurrencySelector(
                selectedCurrency = currency,
                onCurrencySelected = onCurrencyClick,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeScreenPreview() {
    ExchangeScreen(
        state = ExchangeContract.State(

        ),
        onIntent = {}
    )
}