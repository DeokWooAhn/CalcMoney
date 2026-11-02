/// 크래시가 났을 때 직전 상황을 알 수 있도록 단서를 남긴다 (Android `CrashBreadcrumbs` 대응)
///
/// 스택 트레이스만으로는 어느 화면에서 무엇을 하던 중이었는지 알 수 없다. 여기 남긴 값은 다음 크래시
/// 리포트에 함께 실린다. 계산식·금액처럼 사용자가 입력한 값은 남기지 않는다.
public protocol CrashBreadcrumbs: Sendable {
    /// 지금 보고 있는 화면. 크래시 리포트에 키 `CrashBreadcrumbKey.screen`으로 실린다.
    func setScreen(_ screen: String)

    /// 크래시 직전 행동을 한 줄 남긴다.
    func log(_ message: String)
}

/// 크래시 리포트에 싣는 커스텀 키 (Android `CrashBreadcrumbs.SCREEN_KEY` 대응)
public enum CrashBreadcrumbKey {
    public static let screen = "screen"
}

/// 아무것도 남기지 않는 구현. Firebase가 설정되지 않은 개발 빌드와 테스트에서 쓴다.
public struct NoOpCrashBreadcrumbs: CrashBreadcrumbs {
    public init() {}

    public func setScreen(_: String) {}

    public func log(_: String) {}
}
