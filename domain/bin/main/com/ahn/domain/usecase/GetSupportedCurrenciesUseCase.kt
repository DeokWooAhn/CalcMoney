package com.ahn.domain.usecase

import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class GetSupportedCurrenciesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(): List<CurrencyInfo> {
        return repository.getSupportedCurrencies()
    }
}