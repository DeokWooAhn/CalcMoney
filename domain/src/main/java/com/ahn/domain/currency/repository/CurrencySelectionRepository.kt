package com.ahn.domain.currency.repository

import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.model.ExchangeCurrencySelection

interface CurrencySelectionRepository {
    suspend fun getCalculatorSelection(): CalculatorCurrencySelection
    suspend fun saveCalculatorMainCurrencyCode(code: String)
    suspend fun saveCalculatorSubCurrencyCode(code: String)
    suspend fun saveCalculatorSelection(mainCode: String, subCode: String)

    suspend fun getExchangeSelection(): ExchangeCurrencySelection
    suspend fun saveExchangeFromCurrencyCode(code: String)
    suspend fun saveExchangeToCurrencyCode(code: String)
    suspend fun saveExchangeSelection(fromCode: String, toCode: String)
}