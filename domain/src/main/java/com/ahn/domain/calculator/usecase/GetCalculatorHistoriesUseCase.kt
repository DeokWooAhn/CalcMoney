package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCalculatorHistoriesUseCase @Inject constructor(
    private val repository: CalculatorHistoryRepository,
) {
    /**
     * Exposes stored calculator history entries as a reactive stream.
     *
     * @return A Flow that emits the current list of calculator history entries.
     */
    operator fun invoke(): Flow<List<CalculatorHistory>> {
        return repository.getHistories()
    }
}