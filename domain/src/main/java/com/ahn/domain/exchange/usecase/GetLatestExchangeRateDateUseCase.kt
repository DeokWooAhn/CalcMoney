package com.ahn.domain.exchange.usecase

import com.ahn.domain.exchange.repository.ExchangeRateRepository
import javax.inject.Inject

class GetLatestExchangeRateDateUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(): String {
        return repository.getLatestRateDate()
    }
}
