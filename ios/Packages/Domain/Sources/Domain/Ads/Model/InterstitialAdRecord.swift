import Foundation

/// 전면 광고를 보여 줄 차례인지 판단하는 데 쓰는 기록 (Android `InterstitialAdRecord` 대응)
public struct InterstitialAdRecord: Equatable, Sendable {
    /// 계산에 처음 성공한 시각. 이때부터 첫 광고까지의 유예 기간을 센다.
    public var firstCalculatedAt: Date?
    /// 마지막 광고 뒤로 계산에 성공한 횟수.
    public var calculationCountSinceLastAd: Int
    /// 마지막으로 광고를 보여 준 시각. 보여 준 적이 없으면 nil.
    public var lastShownAt: Date?
    /// `lastShownAt`과 같은 날에 광고를 보여 준 횟수.
    public var shownCountOfDay: Int

    public init(
        firstCalculatedAt: Date? = nil,
        calculationCountSinceLastAd: Int = 0,
        lastShownAt: Date? = nil,
        shownCountOfDay: Int = 0,
    ) {
        self.firstCalculatedAt = firstCalculatedAt
        self.calculationCountSinceLastAd = calculationCountSinceLastAd
        self.lastShownAt = lastShownAt
        self.shownCountOfDay = shownCountOfDay
    }

    /// `now`와 같은 날에 광고를 보여 준 횟수. 마지막으로 보여 준 날이 다른 날이면 0이다.
    func shownCount(onSameDayAs now: Date, calendar: Calendar) -> Int {
        guard let lastShownAt, calendar.isDate(lastShownAt, inSameDayAs: now) else { return 0 }

        return shownCountOfDay
    }
}
