package com.ahn.domain.review

import com.ahn.domain.review.model.ReviewPromptRecord
import com.ahn.domain.review.repository.ReviewPromptRepository

/** 메모리에 기록을 들고 있는 테스트용 저장소. */
class FakeReviewPromptRepository(var record: ReviewPromptRecord = ReviewPromptRecord()) : ReviewPromptRepository {
    override suspend fun updateRecord(transform: (ReviewPromptRecord) -> ReviewPromptRecord): ReviewPromptRecord {
        record = transform(record)
        return record
    }
}
