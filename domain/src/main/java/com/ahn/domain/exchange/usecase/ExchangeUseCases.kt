package com.ahn.domain.exchange.usecase

import javax.inject.Inject

class ExchangeUseCases @Inject constructor(
    val exchangeAmount: CalculateExchangeAmountUseCase,
    val convertExchangeAmount: ConvertExchangeAmountUseCase,
    val getExchangeRate: GetExchangeRateUseCase,
    val getSupportedCurrencies: GetSupportedCurrenciesUseCase,
)