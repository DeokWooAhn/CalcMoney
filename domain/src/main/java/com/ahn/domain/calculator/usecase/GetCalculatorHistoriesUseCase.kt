package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCalculatorHistoriesUseCase @Inject constructor(private val repository: CalculatorHistoryRepository) {
    operator fun invoke(): Flow<List<CalculatorHistory>> {
        return repository.getHistories()
    }
}
