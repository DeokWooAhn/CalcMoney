import Foundation

/// 앱을 연 날짜를 기록한다. 같은 날 여러 번 열어도 하루로 센다. (Android `RecordAppOpenUseCase` 대응)
public struct RecordAppOpenUseCase: Sendable {
    private let repository: any ReviewPromptRepository
    private let calendar: Calendar
    private let now: @Sendable () -> Date

    public init(
        repository: any ReviewPromptRepository,
        calendar: Calendar = .current,
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.repository = repository
        self.calendar = calendar
        self.now = now
    }

    public func callAsFunction() async {
        let now = now()
        let calendar = calendar

        await repository.updateRecord { record in
            if let lastOpenedAt = record.lastOpenedAt, calendar.isDate(lastOpenedAt, inSameDayAs: now) {
                return
            }

            record.firstOpenedAt = record.firstOpenedAt ?? now
            record.lastOpenedAt = now
            record.openedDayCount += 1
        }
    }
}
