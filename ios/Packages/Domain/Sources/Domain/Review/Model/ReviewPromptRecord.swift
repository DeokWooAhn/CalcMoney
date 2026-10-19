import Foundation

/// 인앱 리뷰를 요청할 시점을 판단하는 데 쓰는 사용 기록 (Android `ReviewPromptRecord` 대응)
public struct ReviewPromptRecord: Equatable, Sendable {
    /// 앱을 처음 연 시각. 아직 기록이 없으면 nil.
    public var firstOpenedAt: Date?
    /// 앱을 마지막으로 연 시각. 같은 날인지 비교해 사용 일수를 센다.
    public var lastOpenedAt: Date?
    /// 앱을 연 서로 다른 날짜 수. 같은 날 여러 번 열어도 하루로 센다.
    public var openedDayCount: Int
    /// 계산기에서 계산에 성공한 누적 횟수.
    public var calculationSuccessCount: Int
    /// 마지막으로 리뷰를 요청한 시각. 요청한 적이 없으면 nil.
    public var lastRequestedAt: Date?

    public init(
        firstOpenedAt: Date? = nil,
        lastOpenedAt: Date? = nil,
        openedDayCount: Int = 0,
        calculationSuccessCount: Int = 0,
        lastRequestedAt: Date? = nil,
    ) {
        self.firstOpenedAt = firstOpenedAt
        self.lastOpenedAt = lastOpenedAt
        self.openedDayCount = openedDayCount
        self.calculationSuccessCount = calculationSuccessCount
        self.lastRequestedAt = lastRequestedAt
    }
}
