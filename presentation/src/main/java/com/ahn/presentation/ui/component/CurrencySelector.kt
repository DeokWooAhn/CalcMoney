package com.ahn.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.items
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
import com.ahn.presentation.ui.theme.Green

@Composable
fun CurrencySelector(
    selectedCurrency: CurrencyInfo?,
    availableCurrencies: List<CurrencyInfo>,
    onCurrencySelected: (CurrencyInfo) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    textColor: Color = Color.White,
) {
    var showDialog by remember { mutableStateOf(false) }

    selectedCurrency?.let { currency ->
        Row(
            modifier = modifier
                .background(backgroundColor, RoundedCornerShape(12.dp))
                .clickable { showDialog = true }
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
            onDismiss = { showDialog = false },
            onCurrencySelected = { currency ->
                onCurrencySelected(currency)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CurrencyPickerDialog(
    currencies: List<CurrencyInfo>,
    selectedCurrency: CurrencyInfo?,
    onDismiss: () -> Unit,
    onCurrencySelected: (CurrencyInfo) -> Unit,
) {
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
                    items(currencies) { currency ->
                        CurrencyItem(
                            currency = currency,
                            isSelected = currency == selectedCurrency,
                            onClick = { onCurrencySelected(currency) }
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
    onClick: () -> Unit,
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
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

        if (isSelected) {
            Text(
                text = "선택됨",
                fontSize = 12.sp,
                color = Green
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
    )
    var selected by remember { mutableStateOf(sampleCurrencies[0]) }

    CurrencySelector(
        selectedCurrency = selected,
        availableCurrencies = sampleCurrencies,
        onCurrencySelected = { selected = it }
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
            onClick = {}
        )
        CurrencyItem(
            currency = CurrencyInfo(
                "USD",
                "USD",
                "미국 달러",
                "🇺🇸"
            ),
            isSelected = false,
            onClick = {}
        )
    }
}