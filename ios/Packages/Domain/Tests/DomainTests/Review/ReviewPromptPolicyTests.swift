import Foundation
import Testing
@testable import Domain

@Suite("ReviewPromptPolicy")
struct ReviewPromptPolicyTests {
    private static let day: TimeInterval = 24 * 60 * 60
    private static let now = Date(timeIntervalSince1970: 1000 * day)

    /// 모든 조건을 정확히 경계값으로 만족하는 기록
    private static let eligibleRecord = ReviewPromptRecord(
        firstOpenedAt: now.addingTimeInterval(-3 * day),
        lastOpenedAt: now,
        openedDayCount: 3,
        calculationSuccessCount: 5,
        lastRequestedAt: nil,
    )

    @Test("모든 조건을 경계값으로 만족하면 요청해야 한다")
    func 모든_조건을_만족하면_요청한다() {
        #expect(ReviewPromptPolicy.shouldRequest(Self.eligibleRecord, now: Self.now))
    }

    @Test("첫 실행 기록이 없으면 요청하지 않아야 한다")
    func 첫_실행_기록이_없으면_요청하지_않는다() {
        var record = Self.eligibleRecord
        record.firstOpenedAt = nil

        #expect(!ReviewPromptPolicy.shouldRequest(record, now: Self.now))
    }

    @Test("첫 실행 후 3일이 지나지 않았으면 요청하지 않아야 한다")
    func 첫_실행_후_3일이_안_지나면_요청하지_않는다() {
        var record = Self.eligibleRecord
        record.firstOpenedAt = Self.now.addingTimeInterval(-3 * Self.day + 1)

        #expect(!ReviewPromptPolicy.shouldRequest(record, now: Self.now))
    }

    @Test("앱을 연 날이 3일보다 적으면 요청하지 않아야 한다")
    func 연_날이_3일보다_적으면_요청하지_않는다() {
        var record = Self.eligibleRecord
        record.openedDayCount = 2

        #expect(!ReviewPromptPolicy.shouldRequest(record, now: Self.now))
    }

    @Test("계산 성공이 5회보다 적으면 요청하지 않아야 한다")
    func 계산_성공이_5회보다_적으면_요청하지_않는다() {
        var record = Self.eligibleRecord
        record.calculationSuccessCount = 4

        #expect(!ReviewPromptPolicy.shouldRequest(record, now: Self.now))
    }

    @Test("마지막 요청 후 120일이 지나지 않았으면 요청하지 않아야 한다")
    func 마지막_요청_후_120일이_안_지나면_요청하지_않는다() {
        var record = Self.eligibleRecord
        record.lastRequestedAt = Self.now.addingTimeInterval(-120 * Self.day + 1)

        #expect(!ReviewPromptPolicy.shouldRequest(record, now: Self.now))
    }

    @Test("마지막 요청 후 120일이 지났으면 다시 요청해야 한다")
    func 마지막_요청_후_120일이_지나면_다시_요청한다() {
        var record = Self.eligibleRecord
        record.lastRequestedAt = Self.now.addingTimeInterval(-120 * Self.day)

        #expect(ReviewPromptPolicy.shouldRequest(record, now: Self.now))
    }
}
