package com.ahn.domain.exchange.usecase

import com.ahn.domain.exchange.repository.ExchangeRateRepository
import javax.inject.Inject

class GetExchangeRateUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(from: String, to: String): Double {
        return repository.getExchangeRate(from, to)
    }
}