package com.ahn.data.calculator.repository

import com.ahn.data.calculator.local.datasource.CalculatorHistoryDataSource
import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalculatorHistoryRepositoryImpl @Inject constructor(
    private val dataSource: CalculatorHistoryDataSource,
) : CalculatorHistoryRepository {
    override fun getHistories(): Flow<List<CalculatorHistory>> {
        return dataSource.getHistories()
    }

    override suspend fun addHistory(history: CalculatorHistory) {
        dataSource.addHistory(history)
    }

    override suspend fun clearHistories() {
        dataSource.clearHistories()
    }
}
