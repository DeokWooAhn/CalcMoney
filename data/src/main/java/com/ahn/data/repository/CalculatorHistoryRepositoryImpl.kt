package com.ahn.data.repository

import com.ahn.data.local.CalculatorHistoryDataSource
import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalculatorHistoryRepositoryImpl @Inject constructor(
    private val dataSource: CalculatorHistoryDataSource,
) : CalculatorHistoryRepository {
    /**
     * Provides a stream of stored calculator history entries.
     *
     * @return A `Flow` that emits lists of `CalculatorHistory`; each emission represents the current stored histories.
     */
    override fun getHistories(): Flow<List<CalculatorHistory>> {
        return dataSource.getHistories()
    }

    /**
     * Adds a calculator history entry.
     *
     * @param history The history entry to add to storage.
     */
    override suspend fun addHistory(history: CalculatorHistory) {
        dataSource.addHistory(history)
    }

    /**
     * Clears all stored calculator history entries.
     */
    override suspend fun clearHistories() {
        dataSource.clearHistories()
    }
}
