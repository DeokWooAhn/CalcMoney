package com.ahn.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.ahn.presentation.ui.screen.exchange.Currency

@Composable
fun CurrencySelector(
    selectedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black,
    textColor: Color = Color.White,
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = selectedCurrency.flagEmoji,
            fontSize = 24.sp,
        )
        Text(
            text = selectedCurrency.code,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Dropdown Icon",
            tint = textColor,
        )
    }

    if (showDialog) {
        CurrencyPickerDialog(
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
    selectedCurrency: Currency,
    onDismiss: () -> Unit,
    onCurrencySelected: (Currency) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0XFF1E1E1E),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Column {
                Text(
                    text = "통화 선택",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(20.dp),
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

                LazyColumn {
                    items(Currency.entries.toTypedArray()) { currency ->
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
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isSelected) Color.DarkGray else Color.Transparent
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
                text = currency.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
            )
            Text(
                text = currency.currencyName,
                fontSize = 14.sp,
                color = Color.Gray,
            )
        }

        if (isSelected) {
            Text(
                text = "선택됨",
                fontSize = 12.sp,
                color = Color.Green
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun CurrencySelectorPreview() {
    // 상태 관리 시뮬레이션
    var selectedCurrency by remember { mutableStateOf(Currency.KRW) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        CurrencySelector(
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { newCurrency ->
                selectedCurrency = newCurrency
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
fun CurrencyItemPreview() {
    Column {
        // 선택된 상태
        CurrencyItem(
            currency = Currency.KRW,
            isSelected = true,
            onClick = {}
        )
        // 선택되지 않은 상태
        CurrencyItem(
            currency = Currency.USD,
            isSelected = false,
            onClick = {}
        )
    }
}