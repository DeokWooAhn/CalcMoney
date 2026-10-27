import Foundation

/// 계산기를 떠날 때 전면 광고를 보여 줘도 되는지 정하는 규칙 (Android `InterstitialAdPolicy`와 같은 값)
///
/// 처음 써 보는 날에는 띄우지 않고, 계산을 몇 번 해서 도움을 받은 뒤에만, 너무 자주 띄우지 않게 한다.
/// 앱 코드에 실수가 있어도 과하게 뜨지 않도록 AdMob 콘솔의 광고 단위 게재빈도 제한도 함께 건다.
public enum InterstitialAdPolicy {
    /// 계산에 처음 성공한 뒤 첫 광고까지 기다리는 시간
    public static let gracePeriod: TimeInterval = 24 * 60 * 60
    /// 마지막 광고 뒤로 이만큼 계산에 성공해야 다음 광고를 보여 준다.
    public static let minCalculationCount = 4
    /// 광고 사이의 최소 간격
    public static let minInterval: TimeInterval = 5 * 60
    /// 하루에 보여 주는 최대 횟수
    public static let maxShownPerDay = 3

    public static func shouldShow(_ record: InterstitialAdRecord, now: Date, calendar: Calendar) -> Bool {
        guard let firstCalculatedAt = record.firstCalculatedAt else { return false }

        let hasPassedGracePeriod = now.timeIntervalSince(firstCalculatedAt) >= gracePeriod
        let hasCalculatedEnough = record.calculationCountSinceLastAd >= minCalculationCount
        let hasIntervalPassed = record.lastShownAt
            .map { now.timeIntervalSince($0) >= minInterval } ?? true
        let isUnderDailyLimit = record.shownCount(onSameDayAs: now, calendar: calendar) < maxShownPerDay

        return hasPassedGracePeriod && hasCalculatedEnough && hasIntervalPassed && isUnderDailyLimit
    }
}
