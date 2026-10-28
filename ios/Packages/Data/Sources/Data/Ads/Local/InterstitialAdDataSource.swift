import Domain
import Foundation

/// 전면 광고 기록을 UserDefaults에 저장하는 데이터소스 (Android `InterstitialAdDataSource` 대응)
///
/// actor라서 `updateRecord` 안에서 읽고 쓰는 사이에 다른 갱신이 끼어들지 않는다.
public actor InterstitialAdDataSource {
    private enum Key {
        static let firstCalculatedAt = "first_calculated_at"
        static let calculationCountSinceLastAd = "calculation_count_since_last_ad"
        static let lastShownAt = "last_shown_at"
        static let shownCountOfDay = "shown_count_of_day"
    }

    private let userDefaults: UserDefaults

    public init(userDefaults: UserDefaults = .suite(named: "interstitial_ad")) {
        self.userDefaults = userDefaults
    }

    public func record() -> InterstitialAdRecord {
        InterstitialAdRecord(
            firstCalculatedAt: userDefaults.object(forKey: Key.firstCalculatedAt) as? Date,
            calculationCountSinceLastAd: userDefaults.integer(forKey: Key.calculationCountSinceLastAd),
            lastShownAt: userDefaults.object(forKey: Key.lastShownAt) as? Date,
            shownCountOfDay: userDefaults.integer(forKey: Key.shownCountOfDay),
        )
    }

    public func updateRecord(_ transform: @Sendable (inout InterstitialAdRecord) -> Void) {
        var record = record()
        transform(&record)
        save(record)
    }

    private func save(_ record: InterstitialAdRecord) {
        setOrRemove(record.firstCalculatedAt, forKey: Key.firstCalculatedAt)
        userDefaults.set(record.calculationCountSinceLastAd, forKey: Key.calculationCountSinceLastAd)
        setOrRemove(record.lastShownAt, forKey: Key.lastShownAt)
        userDefaults.set(record.shownCountOfDay, forKey: Key.shownCountOfDay)
    }

    private func setOrRemove(_ date: Date?, forKey key: String) {
        if let date {
            userDefaults.set(date, forKey: key)
        } else {
            userDefaults.removeObject(forKey: key)
        }
    }
}
