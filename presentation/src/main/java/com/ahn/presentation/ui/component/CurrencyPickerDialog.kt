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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import com.ahn.presentation.R
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ahn.domain.currency.model.CurrencyInfo

/**
 * Shows a dialog that lets the user pick a currency, displays favorite currencies first in the given sort order, and allows toggling favorites.
 *
 * @param currencies The list of currencies to display.
 * @param selectedCurrency The currently selected currency, or `null` if none.
 * @param favoriteCurrencyCodesForSort Favorite currency codes that determine ordering; favorites appear first in the order provided.
 * @param favoriteCurrencyCodesForIcon Favorite currency codes that should render with the filled favorite icon.
 * @param title The dialog title text.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onCurrencySelected Called with the currency chosen by the user.
 * @param onToggleFavorite Called with a currency code when its favorite icon is toggled.
 */
@Composable
fun CurrencyPickerDialog(
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyInfo?,
    favoriteCurrencyCodesForSort: List<String>,
    favoriteCurrencyCodesForIcon: List<String>,
    title: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val orderMap = remember(favoriteCurrencyCodesForSort) {
        favoriteCurrencyCodesForSort.withIndex().associate { it.value to it.index }
    }

    val sortedCurrencies = remember(currencies, orderMap) {
        val favorites = currencies
            .filter { it.code in orderMap }
            .sortedBy { orderMap[it.code] }

        val others = currencies.filter { it.code !in orderMap }

        favorites + others
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
        ) {
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp),
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                LazyColumn {
                    items(sortedCurrencies, key = { it.code }) { currency ->
                        CurrencyPickerItem(
                            currency = currency,
                            isSelected = currency == selectedCurrency,
                            isFavorite = currency.code in favoriteCurrencyCodesForIcon,
                            onClick = { onCurrencySelected(currency) },
                            onToggleFavorite = { onToggleFavorite(currency.code) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a selectable currency row showing the flag, currency code and name, and a favorite toggle.
 *
 * The row is visually highlighted when `isSelected` is true. The favorite icon appears filled when
 * `isFavorite` is true. Tapping the row invokes `onClick`; tapping the favorite icon invokes
 * `onToggleFavorite`.
 *
 * @param currency The currency data to display.
 * @param isSelected Whether this row represents the currently selected currency.
 * @param isFavorite Whether the currency is marked as a favorite (controls icon appearance).
 * @param onClick Called when the row is clicked.
 * @param onToggleFavorite Called when the favorite icon is clicked.
 */
@Composable
private fun CurrencyPickerItem(
    currency: CurrencyInfo,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                }
            )
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.flagEmoji,
            fontSize = 24.sp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.displayCode,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = currency.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = if (isFavorite) {
                    Icons.Default.Favorite
                } else {
                    Icons.Default.FavoriteBorder
                },
                contentDescription = if (isFavorite) {
                    stringResource(R.string.remove_favorite)
                } else {
                    stringResource(R.string.add_favorite)
                },
                tint = if (isFavorite) {
                    Color.Red
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
            )
        }
    }
}