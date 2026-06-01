package com.ahn.data.currency.repository

import com.ahn.data.currency.local.datasource.CurrencySelectionDataSource
import com.ahn.domain.currency.repository.CurrencySelectionRepository
import javax.inject.Inject

class CurrencySelectionRepositoryImpl @Inject constructor(private val dataSource: CurrencySelectionDataSource) :
    CurrencySelectionRepository {
    override suspend fun getCalculatorSelection() = dataSource.getCalculatorSelection()

    override suspend fun saveCalculatorMainCurrencyCode(code: String) {
        dataSource.saveCalculatorMainCurrencyCode(code)
    }

    override suspend fun saveCalculatorSubCurrencyCode(code: String) {
        dataSource.saveCalculatorSubCurrencyCode(code)
    }

    override suspend fun saveCalculatorSelection(mainCode: String, subCode: String) {
        dataSource.saveCalculatorSelection(mainCode, subCode)
    }

    override suspend fun getExchangeSelection() = dataSource.getExchangeSelection()

    override suspend fun saveExchangeFromCurrencyCode(code: String) {
        dataSource.saveExchangeFromCurrencyCode(code)
    }

    override suspend fun saveExchangeToCurrencyCode(code: String) {
        dataSource.saveExchangeToCurrencyCode(code)
    }

    override suspend fun saveExchangeSelection(fromCode: String, toCode: String) {
        dataSource.saveExchangeSelection(fromCode, toCode)
    }
}
