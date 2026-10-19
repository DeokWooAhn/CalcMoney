package com.ahn.domain.review.repository

import com.ahn.domain.review.model.ReviewPromptRecord

interface ReviewPromptRepository {
    /**
     * 저장된 기록을 [transform]으로 고쳐 저장하고, 저장한 기록을 반환합니다.
     *
     * 읽고 고쳐 쓰는 동안 다른 갱신이 끼어들지 않도록 한 번에 처리합니다.
     */
    suspend fun updateRecord(transform: (ReviewPromptRecord) -> ReviewPromptRecord): ReviewPromptRecord
}
