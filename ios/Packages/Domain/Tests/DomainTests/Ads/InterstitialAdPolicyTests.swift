import Foundation
import Testing
@testable import Domain

/// 메모리에 기록을 들고 있는 테스트용 저장소
actor InMemoryInterstitialAdRepository: InterstitialAdRepository {
    private(set) var current: InterstitialAdRecord

    init(record: InterstitialAdRecord = InterstitialAdRecord()) {
        current = record
    }

    func record() async -> InterstitialAdRecord {
        current
    }

    func updateRecord(_ transform: @Sendable (inout InterstitialAdRecord) -> Void) async {
        transform(&current)
    }
}

/// 기기 시간대와 관계없이 같은 결과가 나오도록 UTC 달력으로 날짜를 나눈다.
let utcCalendar: Calendar = {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(identifier: "UTC")!
    return calendar
}()

@Suite("InterstitialAdPolicy")
struct InterstitialAdPolicyTests {
    private static let hour: TimeInterval = 60 * 60
    private static let minute: TimeInterval = 60
    /// UTC 정오. 하루 경계와 멀어서 몇 분·몇 시간을 빼도 같은 날이다.
    private static let now = Date(timeIntervalSince1970: 1000 * 24 * hour + 12 * hour)

    /// 모든 조건을 정확히 경계값으로 만족하는 기록
    private static let eligibleRecord = InterstitialAdRecord(
        firstCalculatedAt: now.addingTimeInterval(-24 * hour),
        calculationCountSinceLastAd: 4,
        lastShownAt: now.addingTimeInterval(-5 * minute),
        shownCountOfDay: 2,
    )

    private func shouldShow(_ record: InterstitialAdRecord) -> Bool {
        InterstitialAdPolicy.shouldShow(record, now: Self.now, calendar: utcCalendar)
    }

    @Test("모든 조건을 경계값으로 만족하면 보여 줘야 한다")
    func 모든_조건을_만족하면_보여_준다() {
        #expect(shouldShow(Self.eligibleRecord))
    }

    @Test("광고를 보여 준 적이 없고 유예 기간과 계산 횟수를 채웠으면 보여 줘야 한다")
    func 첫_광고는_유예_기간과_계산_횟수만_본다() {
        var record = Self.eligibleRecord
        record.lastShownAt = nil
        record.shownCountOfDay = 0

        #expect(shouldShow(record))
    }

    @Test("계산한 기록이 없으면 보여 주지 않아야 한다")
    func 계산_기록이_없으면_보여_주지_않는다() {
        #expect(!shouldShow(InterstitialAdRecord()))
    }

    @Test("처음 계산하고 24시간이 지나지 않았으면 보여 주지 않아야 한다")
    func 유예_기간_안에는_보여_주지_않는다() {
        var record = Self.eligibleRecord
        record.firstCalculatedAt = Self.now.addingTimeInterval(-24 * Self.hour + 1)

        #expect(!shouldShow(record))
    }

    @Test("마지막 광고 뒤로 계산이 4번보다 적으면 보여 주지 않아야 한다")
    func 계산이_4번보다_적으면_보여_주지_않는다() {
        var record = Self.eligibleRecord
        record.calculationCountSinceLastAd = 3

        #expect(!shouldShow(record))
    }

    @Test("마지막 광고 뒤로 5분이 지나지 않았으면 보여 주지 않아야 한다")
    func 간격이_5분보다_짧으면_보여_주지_않는다() {
        var record = Self.eligibleRecord
        record.lastShownAt = Self.now.addingTimeInterval(-5 * Self.minute + 1)

        #expect(!shouldShow(record))
    }

    @Test("오늘 이미 3번 보여 줬으면 보여 주지 않아야 한다")
    func 하루_3번을_넘기지_않는다() {
        var record = Self.eligibleRecord
        record.shownCountOfDay = 3

        #expect(!shouldShow(record))
    }

    @Test("어제 3번 보여 줬으면 오늘은 처음부터 세므로 보여 줘야 한다")
    func 날짜가_바뀌면_하루_횟수를_다시_센다() {
        var record = Self.eligibleRecord
        record.lastShownAt = Self.now.addingTimeInterval(-24 * Self.hour)
        record.shownCountOfDay = 3

        #expect(shouldShow(record))
    }
}

@Suite("InterstitialAdUseCases")
struct InterstitialAdUseCasesTests {
    private static let now = Date(timeIntervalSince1970: 1000 * 24 * 60 * 60 + 12 * 60 * 60)
    private static let day: TimeInterval = 24 * 60 * 60

    @Test("처음 계산을 기록하면 처음 계산한 시각을 남기고 횟수를 1로 한다")
    func 첫_계산_기록() async {
        let repository = InMemoryInterstitialAdRepository()

        await RecordInterstitialAdCalculationUseCase(repository: repository, now: { Self.now })()

        let record = await repository.current
        #expect(record.firstCalculatedAt == Self.now)
        #expect(record.calculationCountSinceLastAd == 1)
    }

    @Test("이미 계산한 적이 있으면 처음 계산한 시각은 그대로 두고 횟수만 늘린다")
    func 이어지는_계산_기록() async {
        let firstCalculatedAt = Self.now.addingTimeInterval(-2 * Self.day)
        let repository = InMemoryInterstitialAdRepository(
            record: InterstitialAdRecord(firstCalculatedAt: firstCalculatedAt, calculationCountSinceLastAd: 3),
        )

        await RecordInterstitialAdCalculationUseCase(repository: repository, now: { Self.now })()

        let record = await repository.current
        #expect(record.firstCalculatedAt == firstCalculatedAt)
        #expect(record.calculationCountSinceLastAd == 4)
    }

    @Test("조건을 채웠으면 보여 줄 차례라고 알리고, 보여 준 뒤에는 다시 기다리게 한다")
    func 보여_준_뒤에는_다시_기다린다() async {
        let eligibleRecord = InterstitialAdRecord(
            firstCalculatedAt: Self.now.addingTimeInterval(-2 * Self.day),
            calculationCountSinceLastAd: 4,
        )
        let repository = InMemoryInterstitialAdRepository(record: eligibleRecord)
        let shouldShow = ShouldShowInterstitialAdUseCase(
            repository: repository,
            calendar: utcCalendar,
            now: { Self.now },
        )

        #expect(await shouldShow())
        #expect(await repository.current == eligibleRecord)

        await RecordInterstitialAdShownUseCase(repository: repository, calendar: utcCalendar, now: { Self.now })()

        let record = await repository.current
        #expect(record.calculationCountSinceLastAd == 0)
        #expect(record.lastShownAt == Self.now)
        #expect(record.shownCountOfDay == 1)
        #expect(await !shouldShow())
    }

    @Test("오늘 이미 보여 줬으면 오늘 횟수를 이어서 센다")
    func 같은_날에는_횟수를_이어_센다() async {
        let repository = InMemoryInterstitialAdRepository(
            record: InterstitialAdRecord(lastShownAt: Self.now.addingTimeInterval(-60 * 60), shownCountOfDay: 2),
        )

        await RecordInterstitialAdShownUseCase(repository: repository, calendar: utcCalendar, now: { Self.now })()

        #expect(await repository.current.shownCountOfDay == 3)
    }

    @Test("어제 보여 줬으면 오늘 횟수를 1부터 다시 센다")
    func 날짜가_바뀌면_횟수를_다시_센다() async {
        let repository = InMemoryInterstitialAdRepository(
            record: InterstitialAdRecord(lastShownAt: Self.now.addingTimeInterval(-Self.day), shownCountOfDay: 3),
        )

        await RecordInterstitialAdShownUseCase(repository: repository, calendar: utcCalendar, now: { Self.now })()

        #expect(await repository.current.shownCountOfDay == 1)
    }
}
