package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import javax.inject.Inject

class AddCalculatorHistoryUseCase @Inject constructor(
    private val repository: CalculatorHistoryRepository,
) {
    suspend operator fun invoke(history: CalculatorHistory) {
        repository.addHistory(history)
    }
}
