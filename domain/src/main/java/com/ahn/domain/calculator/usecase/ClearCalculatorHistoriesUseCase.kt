package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import javax.inject.Inject

class ClearCalculatorHistoriesUseCase @Inject constructor(
    private val repository: CalculatorHistoryRepository,
) {
    suspend operator fun invoke() {
        repository.clearHistories()
    }
}