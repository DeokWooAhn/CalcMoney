package com.ahn.domain.currency.model

data class CalculatorCurrencySelection(
    val mainCode: String?,
    val subCode: String?,
)

data class ExchangeCurrencySelection(
    val fromCode: String?,
    val toCode: String?,
)