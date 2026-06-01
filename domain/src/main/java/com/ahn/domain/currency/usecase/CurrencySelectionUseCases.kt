package com.ahn.domain.currency.usecase

import javax.inject.Inject

class CurrencySelectionUseCases @Inject constructor(
    val getCalculatorSelection: GetCalculatorSelectionUseCase,
    val saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase,
    val saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase,
    val saveCalculatorSelection: SaveCalculatorSelectionUseCase,
    val getExchangeSelection: GetExchangeSelectionUseCase,
    val saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase,
    val saveExchangeToCurrency: SaveExchangeToCurrencyUseCase,
    val saveExchangeSelection: SaveExchangeSelectionUseCase,
)
