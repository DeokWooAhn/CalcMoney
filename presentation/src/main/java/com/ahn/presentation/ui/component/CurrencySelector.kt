package com.ahn.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R

/**
 * Displays a currency chip showing the currently selected currency (flag and code) or a prompt to select a currency, and opens a currency picker dialog when tapped.
 *
 * If `selectedCurrency` is null, a prompt text is shown; otherwise the chip shows the currency's flag, code, and a dropdown icon. Tapping the chip opens a dialog populated with `availableCurrencies` and current favorites.
 *
 * @param selectedCurrency The currently selected currency, or `null` to show the selection prompt.
 * @param availableCurrencies The list of currencies shown in the picker dialog.
 * @param favoriteCurrencyCodes Currency codes marked as favorites; used to indicate and sort favorites in the dialog.
 * @param onCurrencySelected Callback invoked with the currency chosen in the dialog.
 * @param onToggleFavorite Callback invoked with a currency code when its favorite state is toggled.
 * @param modifier Modifier applied to the chip or prompt.
 * @param backgroundColor Background color for the chip.
 * @param textColor Text and icon color for the chip and prompt.
 * @param dialogTitle Title text displayed at the top of the currency picker dialog.
 */
@Composable
fun CurrencySelector(
    selectedCurrency: CurrencyInfo?,
    availableCurrencies: List<CurrencyInfo>,
    favoriteCurrencyCodes: List<String>,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    textColor: Color = Color.White,
    dialogTitle: String = stringResource(R.string.select_currency),
) {
    var showDialog by remember { mutableStateOf(false) }
    var favoriteCodesSnapshot by remember { mutableStateOf<List<String>>(emptyList()) }

    selectedCurrency?.let { currency ->
        Row(
            modifier = modifier
                .background(backgroundColor, RoundedCornerShape(12.dp))
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
                contentDescription = "Dropdown Icon",
                tint = textColor,
            )
        }
    } ?: run {
        Text(
            text = stringResource(R.string.select_currency),
            color = textColor,
            modifier = modifier.padding(12.dp, 8.dp)
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

/**
 * Preview of CurrencySelector demonstrating selection and favorites behavior with sample currencies.
 *
 * Initializes three sample currencies (KRW, USD, JPY), sets KRW as the initially selected currency,
 * and marks USD and JPY as initial favorites. Interactions update the selected currency and toggle
 * favorite membership for the corresponding currency code.
 */
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