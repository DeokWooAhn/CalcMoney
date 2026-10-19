import Foundation
import Testing
@testable import Domain

@Suite("RecordAppOpenUseCase")
struct RecordAppOpenUseCaseTests {
    private static func calendar(_ identifier: String) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: identifier)!
        return calendar
    }

    private static func date(_ iso8601: String) -> Date {
        ISO8601DateFormatter().date(from: iso8601)!
    }

    private static func makeUseCase(
        _ repository: InMemoryReviewPromptRepository,
        at iso8601: String,
        timeZone: String = "Asia/Seoul",
    ) -> RecordAppOpenUseCase {
        let now = date(iso8601)
        return RecordAppOpenUseCase(repository: repository, calendar: calendar(timeZone), now: { now })
    }

    @Test("처음 열면 첫 실행 시각을 남기고 사용 일수를 1로 만들어야 한다")
    func 처음_열면_첫_실행을_기록한다() async {
        let repository = InMemoryReviewPromptRepository()

        await Self.makeUseCase(repository, at: "2026-10-20T01:00:00Z")()

        let now = Self.date("2026-10-20T01:00:00Z")
        #expect(await repository.record == ReviewPromptRecord(
            firstOpenedAt: now,
            lastOpenedAt: now,
            openedDayCount: 1,
        ))
    }

    @Test("같은 날 다시 열면 기록이 바뀌지 않아야 한다")
    func 같은_날_다시_열면_기록이_그대로다() async {
        let existing = ReviewPromptRecord(
            firstOpenedAt: Self.date("2026-10-18T01:00:00Z"),
            lastOpenedAt: Self.date("2026-10-20T01:00:00Z"),
            openedDayCount: 2,
        )
        let repository = InMemoryReviewPromptRepository(record: existing)

        await Self.makeUseCase(repository, at: "2026-10-20T14:00:00Z")()

        #expect(await repository.record == existing)
    }

    @Test("다음 날 열면 첫 실행 시각은 두고 사용 일수를 하나 늘려야 한다")
    func 다음_날_열면_사용_일수가_늘어난다() async {
        let firstOpenedAt = Self.date("2026-10-18T01:00:00Z")
        let repository = InMemoryReviewPromptRepository(record: ReviewPromptRecord(
            firstOpenedAt: firstOpenedAt,
            lastOpenedAt: Self.date("2026-10-19T01:00:00Z"),
            openedDayCount: 2,
        ))

        await Self.makeUseCase(repository, at: "2026-10-20T01:00:00Z")()

        let record = await repository.record
        #expect(record.firstOpenedAt == firstOpenedAt)
        #expect(record.lastOpenedAt == Self.date("2026-10-20T01:00:00Z"))
        #expect(record.openedDayCount == 3)
    }

    @Test("UTC로는 같은 날이어도 기기 시간대로 날짜가 바뀌면 다른 날로 세야 한다")
    func 기기_시간대_기준으로_날짜를_센다() async {
        // 2026-10-19T14:30Z는 서울 기준 10월 19일 23:30, 2026-10-19T15:30Z는 10월 20일 00:30이다.
        let repository = InMemoryReviewPromptRepository()

        await Self.makeUseCase(repository, at: "2026-10-19T14:30:00Z")()
        await Self.makeUseCase(repository, at: "2026-10-19T15:30:00Z")()

        #expect(await repository.record.openedDayCount == 2)
    }

    @Test("UTC 시간대 기기에서는 같은 두 시각을 같은 날로 세야 한다")
    func UTC_기기에서는_같은_날로_센다() async {
        let repository = InMemoryReviewPromptRepository()

        await Self.makeUseCase(repository, at: "2026-10-19T14:30:00Z", timeZone: "UTC")()
        await Self.makeUseCase(repository, at: "2026-10-19T15:30:00Z", timeZone: "UTC")()

        #expect(await repository.record.openedDayCount == 1)
    }
}
