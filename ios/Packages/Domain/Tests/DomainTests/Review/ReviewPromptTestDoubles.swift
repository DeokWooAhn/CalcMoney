@testable import Domain

/// 메모리에 기록을 들고 있는 테스트용 저장소
actor InMemoryReviewPromptRepository: ReviewPromptRepository {
    private(set) var record: ReviewPromptRecord

    init(record: ReviewPromptRecord = ReviewPromptRecord()) {
        self.record = record
    }

    func updateRecord<Result: Sendable>(
        _ transform: @Sendable (inout ReviewPromptRecord) -> Result,
    ) async -> Result {
        transform(&record)
    }
}
