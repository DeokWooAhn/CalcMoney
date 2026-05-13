package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import javax.inject.Inject

class ClearCalculatorHistoriesUseCase @Inject constructor(
    private val repository: CalculatorHistoryRepository,
) {
    /**
     * Clears all stored calculator history entries.
     *
     * Any exceptions thrown by the underlying data layer will propagate to the caller.
     */
    suspend operator fun invoke() {
        repository.clearHistories()
    }
}