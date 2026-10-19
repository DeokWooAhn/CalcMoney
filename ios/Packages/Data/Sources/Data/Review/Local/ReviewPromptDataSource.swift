import Domain
import Foundation

/// 인앱 리뷰 요청 기록을 UserDefaults에 저장하는 데이터소스 (Android `ReviewPromptDataSource` 대응)
///
/// actor라서 `updateRecord` 안에서 읽고 쓰는 사이에 다른 갱신이 끼어들지 않는다.
public actor ReviewPromptDataSource {
    private enum Key {
        static let firstOpenedAt = "first_opened_at"
        static let lastOpenedAt = "last_opened_at"
        static let openedDayCount = "opened_day_count"
        static let calculationSuccessCount = "calculation_success_count"
        static let lastRequestedAt = "last_requested_at"
    }

    private let userDefaults: UserDefaults

    public init(userDefaults: UserDefaults = .suite(named: "review_prompt")) {
        self.userDefaults = userDefaults
    }

    public func updateRecord<Result: Sendable>(
        _ transform: @Sendable (inout ReviewPromptRecord) -> Result,
    ) -> Result {
        var record = currentRecord()
        let result = transform(&record)
        save(record)

        return result
    }

    private func currentRecord() -> ReviewPromptRecord {
        ReviewPromptRecord(
            firstOpenedAt: userDefaults.object(forKey: Key.firstOpenedAt) as? Date,
            lastOpenedAt: userDefaults.object(forKey: Key.lastOpenedAt) as? Date,
            openedDayCount: userDefaults.integer(forKey: Key.openedDayCount),
            calculationSuccessCount: userDefaults.integer(forKey: Key.calculationSuccessCount),
            lastRequestedAt: userDefaults.object(forKey: Key.lastRequestedAt) as? Date,
        )
    }

    private func save(_ record: ReviewPromptRecord) {
        setOrRemove(record.firstOpenedAt, forKey: Key.firstOpenedAt)
        setOrRemove(record.lastOpenedAt, forKey: Key.lastOpenedAt)
        userDefaults.set(record.openedDayCount, forKey: Key.openedDayCount)
        userDefaults.set(record.calculationSuccessCount, forKey: Key.calculationSuccessCount)
        setOrRemove(record.lastRequestedAt, forKey: Key.lastRequestedAt)
    }

    private func setOrRemove(_ date: Date?, forKey key: String) {
        if let date {
            userDefaults.set(date, forKey: key)
        } else {
            userDefaults.removeObject(forKey: key)
        }
    }
}
