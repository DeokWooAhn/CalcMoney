package com.ahn.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.domain.model.CurrencyInfo

@Composable
fun ExchangeInputContainer(
    amount: String,
    currency: CurrencyInfo?,
    onAmountChange: (String) -> Unit,
    onCurrencyClick: (CurrencyInfo) -> Unit,
    availableCurrencies: List<CurrencyInfo>,
    favoriteCurrencyCodes: List<String>,
    onToggleFavorite: (String) -> Unit,
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

            currency?.let {
                CurrencySelector(
                    selectedCurrency = currency,
                    availableCurrencies = availableCurrencies,
                    favoriteCurrencyCodes = favoriteCurrencyCodes,
                    onCurrencySelected = onCurrencyClick,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExchangeInputContainerPreview() {
    MaterialTheme {
        ExchangeInputContainer(
            amount = "1000",
            currency = CurrencyInfo(code = "USD", displayCode = "USD", name = "미국 달러", flagEmoji = "🇺🇸"),
            onAmountChange = {},
            onCurrencyClick = {},
            availableCurrencies = emptyList(),
            favoriteCurrencyCodes = emptyList(),
            onToggleFavorite = {},
            isEditable = true,
            label = "보낼 금액"
        )
    }
}