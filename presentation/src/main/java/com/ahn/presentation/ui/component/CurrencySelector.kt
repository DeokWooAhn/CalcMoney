package com.ahn.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import com.ahn.presentation.ui.theme.currencySelectorBorder
import com.ahn.presentation.ui.theme.currencySelectorSurface

@Composable
fun CurrencySelector(
    selectedCurrency: CurrencyInfo?,
    availableCurrencies: List<CurrencyInfo>,
    favoriteCurrencyCodes: List<String>,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.currencySelectorSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    dialogTitle: String = stringResource(R.string.select_currency),
) {
    var showDialog by remember { mutableStateOf(false) }
    var favoriteCodesSnapshot by remember { mutableStateOf<List<String>>(emptyList()) }
    val shape = RoundedCornerShape(12.dp)

    selectedCurrency?.let { currency ->
        Row(
            modifier = modifier
                .background(backgroundColor, shape)
                .border(1.dp, MaterialTheme.colorScheme.currencySelectorBorder, shape)
                .clickable {
                    showDialog = true
                    favoriteCodesSnapshot = favoriteCurrencyCodes
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedCurrency.flagEmoji,
                fontSize = 20.sp,
            )
            Text(
                text = selectedCurrency.code,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = textColor,
            )
        }
    } ?: run {
        Text(
            text = stringResource(R.string.select_currency),
            color = textColor,
            modifier = modifier.padding(12.dp, 8.dp),
        )
    }

    if (showDialog) {
        CurrencyPickerDialog(
            currencies = availableCurrencies,
            selectedCurrency = selectedCurrency,
            favoriteCurrencyCodesForSort = favoriteCodesSnapshot,
            favoriteCurrencyCodesForIcon = favoriteCurrencyCodes,
            title = dialogTitle,
            onDismiss = { showDialog = false },
            onCurrencySelected = { currency ->
                onCurrencySelected(currency)
                showDialog = false
            },
            onToggleFavorite = onToggleFavorite,
        )
    }
}

@Preview
@Composable
fun CurrencySelectorPreview() {
    val sampleCurrencies = listOf(
        CurrencyInfo("KRW", "KRW", stringResource(R.string.preview_currency_krw), "🇰🇷"),
        CurrencyInfo("USD", "USD", stringResource(R.string.preview_currency_usd), "🇺🇸"),
        CurrencyInfo("JPY", "JPY", stringResource(R.string.preview_currency_jpy), "🇯🇵"),
    )

    var selected by remember { mutableStateOf(sampleCurrencies[0]) }
    var favorites by remember { mutableStateOf(listOf("USD", "JPY")) }

    CurrencySelector(
        selectedCurrency = selected,
        availableCurrencies = sampleCurrencies,
        favoriteCurrencyCodes = favorites,
        onCurrencySelected = { selected = it },
        onToggleFavorite = { code ->
            favorites = if (code in favorites) favorites - code else favorites + code
        },
    )
}
