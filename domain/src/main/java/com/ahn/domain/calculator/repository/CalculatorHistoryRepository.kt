package com.ahn.domain.calculator.repository

import com.ahn.domain.calculator.model.CalculatorHistory
import kotlinx.coroutines.flow.Flow

interface CalculatorHistoryRepository {
    fun getHistories(): Flow<List<CalculatorHistory>>
    suspend fun saveHistories(histories: List<CalculatorHistory>)
    suspend fun clearHistories()
}