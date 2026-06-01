package com.ahn.domain.exchange.usecase

import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.repository.ExchangeRateRepository
import javax.inject.Inject

class GetSupportedCurrenciesUseCase @Inject constructor(private val repository: ExchangeRateRepository) {
    suspend operator fun invoke(): List<CurrencyInfo> {
        return repository.getSupportedCurrencies()
    }
}
