import Foundation

/// 인앱 리뷰를 요청해도 되는지 정하는 규칙 (Android `ReviewPromptPolicy`와 같은 값)
///
/// 충분히 써 본 사용자에게만, 너무 자주 묻지 않게 요청한다. 실제로 창을 띄울지는 OS·스토어가 다시 정하므로
/// (iOS는 1년에 3번까지만 띄우고 Play는 짧은 기간 안의 재요청을 무시한다) 이 규칙은 요청 횟수를 줄이는 쪽으로 잡는다.
public enum ReviewPromptPolicy {
    private static let day: TimeInterval = 24 * 60 * 60

    public static let minTimeSinceFirstOpen: TimeInterval = 3 * day
    public static let minOpenedDayCount = 3
    public static let minCalculationSuccessCount = 5
    /// iOS의 1년 3회 제한과 맞춘 요청 간격
    public static let requestInterval: TimeInterval = 120 * day

    public static func shouldRequest(_ record: ReviewPromptRecord, now: Date) -> Bool {
        guard let firstOpenedAt = record.firstOpenedAt else { return false }

        let hasUsedLongEnough = now.timeIntervalSince(firstOpenedAt) >= minTimeSinceFirstOpen
        let hasOpenedOnEnoughDays = record.openedDayCount >= minOpenedDayCount
        let hasCalculatedEnough = record.calculationSuccessCount >= minCalculationSuccessCount
        let hasIntervalPassed = record.lastRequestedAt
            .map { now.timeIntervalSince($0) >= requestInterval } ?? true

        return hasUsedLongEnough && hasOpenedOnEnoughDays && hasCalculatedEnough && hasIntervalPassed
    }
}
