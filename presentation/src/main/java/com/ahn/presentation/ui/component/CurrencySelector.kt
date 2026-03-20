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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ahn.domain.model.CurrencyInfo

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
            text = "통화 선택",
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
            onDismiss = { showDialog = false },
            onCurrencySelected = { currency ->
                onCurrencySelected(currency)
                showDialog = false
            },
            onToggleFavorite = onToggleFavorite,
        )
    }
}

@Composable
private fun CurrencyPickerDialog(
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyInfo?,
    favoriteCurrencyCodesForSort: List<String>,
    favoriteCurrencyCodesForIcon: List<String>,
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
                .heightIn(max = 500.dp)
        ) {
            Column {
                Text(
                    text = "통화 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(20.dp),
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                LazyColumn {
                    items(sortedCurrencies, key = { it.code }) { currency ->
                        CurrencyItem(
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

@Composable
private fun CurrencyItem(
    currency: CurrencyInfo,
    isSelected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    Color.Transparent
            )
            .padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = currency.flagEmoji,
            fontSize = 28.sp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = currency.displayCode,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = currency.name,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "즐겨찾기 해제" else "즐겨찾기 추가",
                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Preview
@Composable
fun CurrencySelectorPreview() {
    val sampleCurrencies = listOf(
        CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷"),
        CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸"),
        CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵"),
    )

    var selected by remember { mutableStateOf(sampleCurrencies[0]) }
    var favorites by remember { mutableStateOf(listOf("USD", "JPY")) }

    CurrencySelector(
        selectedCurrency = selected,
        availableCurrencies = sampleCurrencies,
        favoriteCurrencyCodes = favorites,
        onCurrencySelected = { selected = it },
        onToggleFavorite = { code ->
            favorites = if (code in favorites) {
                favorites - code
            } else {
                favorites + code
            }
        }
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
fun CurrencyItemPreview() {
    Column {
        // 선택된 상태
        CurrencyItem(
            currency = CurrencyInfo(
                "KRW",
                "KRW",
                "대한민국 원",
                "🇰🇷"
            ),
            isSelected = true,
            isFavorite = true,
            onClick = {},
            onToggleFavorite = {}
        )
        CurrencyItem(
            currency = CurrencyInfo(
                "USD",
                "USD",
                "미국 달러",
                "🇺🇸"
            ),
            isSelected = false,
            isFavorite = true,
            onClick = {},
            onToggleFavorite = {}
        )
    }
}