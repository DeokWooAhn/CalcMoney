package com.ahn.data.review.repository

import com.ahn.data.review.local.datasource.ReviewPromptDataSource
import com.ahn.domain.review.model.ReviewPromptRecord
import com.ahn.domain.review.repository.ReviewPromptRepository
import javax.inject.Inject

class ReviewPromptRepositoryImpl @Inject constructor(
    private val dataSource: ReviewPromptDataSource,
) : ReviewPromptRepository {
    override suspend fun updateRecord(transform: (ReviewPromptRecord) -> ReviewPromptRecord): ReviewPromptRecord {
        return dataSource.updateRecord(transform)
    }
}
