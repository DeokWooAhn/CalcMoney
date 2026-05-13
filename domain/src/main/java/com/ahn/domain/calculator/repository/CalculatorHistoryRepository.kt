package com.ahn.domain.calculator.repository

import com.ahn.domain.calculator.model.CalculatorHistory
import kotlinx.coroutines.flow.Flow

interface CalculatorHistoryRepository {
    /**
 * Observes calculator history as a stream of list snapshots.
 *
 * @return A Flow that emits the current list of CalculatorHistory; each emission represents the latest stored histories and may emit again when the collection changes.
 */
fun getHistories(): Flow<List<CalculatorHistory>>
    /**
 * Adds a calculator history entry to the repository.
 *
 * @param history The `CalculatorHistory` entry to store.
 */
suspend fun addHistory(history: CalculatorHistory)
    /**
 * Removes all stored calculator history entries from the repository.
 *
 * This operation deletes every `CalculatorHistory` record so subsequent calls to [getHistories]
 * will emit an empty list until new entries are added.
 */
suspend fun clearHistories()
}
