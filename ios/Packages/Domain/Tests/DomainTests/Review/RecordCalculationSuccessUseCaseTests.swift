import Foundation
import Testing
@testable import Domain

@Suite("RecordCalculationSuccessUseCase")
struct RecordCalculationSuccessUseCaseTests {
    private static let day: TimeInterval = 24 * 60 * 60
    private static let now = Date(timeIntervalSince1970: 1000 * day)

    /// 계산 성공 1회만 더하면 요청 조건을 모두 만족하는 기록
    private static let almostEligibleRecord = ReviewPromptRecord(
        firstOpenedAt: now.addingTimeInterval(-10 * day),
        lastOpenedAt: now,
        openedDayCount: 3,
        calculationSuccessCount: 4,
    )

    private static func makeUseCase(_ repository: InMemoryReviewPromptRepository) -> RecordCalculationSuccessUseCase {
        let now = now
        return RecordCalculationSuccessUseCase(repository: repository, now: { now })
    }

    @Test("조건을 아직 만족하지 못하면 횟수만 늘리고 요청하지 않아야 한다")
    func 조건_미달이면_횟수만_늘린다() async {
        var record = Self.almostEligibleRecord
        record.calculationSuccessCount = 2
        let repository = InMemoryReviewPromptRepository(record: record)

        let shouldRequest = await Self.makeUseCase(repository)()

        #expect(!shouldRequest)
        #expect(await repository.record.calculationSuccessCount == 3)
        #expect(await repository.record.lastRequestedAt == nil)
    }

    @Test("이번 계산으로 조건을 만족하면 요청해야 하고 요청 시각을 남겨야 한다")
    func 조건을_만족하면_요청하고_시각을_남긴다() async {
        let repository = InMemoryReviewPromptRepository(record: Self.almostEligibleRecord)

        let shouldRequest = await Self.makeUseCase(repository)()

        #expect(shouldRequest)
        #expect(await repository.record.calculationSuccessCount == 5)
        #expect(await repository.record.lastRequestedAt == Self.now)
    }

    @Test("방금 요청했으면 같은 시각의 다음 계산에서 다시 요청하지 않아야 한다")
    func 방금_요청했으면_다시_요청하지_않는다() async {
        let repository = InMemoryReviewPromptRepository(record: Self.almostEligibleRecord)
        let useCase = Self.makeUseCase(repository)

        _ = await useCase()
        let shouldRequestAgain = await useCase()

        #expect(!shouldRequestAgain)
        #expect(await repository.record.calculationSuccessCount == 6)
        #expect(await repository.record.lastRequestedAt == Self.now)
    }
}
