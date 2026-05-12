package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import javax.inject.Inject

class SaveCalculatorHistoriesUseCase @Inject constructor(
    private val repository: CalculatorHistoryRepository
) {
    suspend operator fun invoke(histories: List<CalculatorHistory>) {
        repository.saveHistories(histories.takeLast(20))
    }
}