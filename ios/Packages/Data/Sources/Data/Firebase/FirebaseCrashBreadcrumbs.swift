import Domain
import FirebaseCrashlytics

/// Firebase Crashlytics의 커스텀 키와 로그로 크래시 단서를 남긴다 (Android `FirebaseCrashBreadcrumbs` 대응)
///
/// Firebase를 초기화한 뒤에만 만든다. 초기화 전에 Crashlytics를 부르면 크래시한다.
public struct FirebaseCrashBreadcrumbs: CrashBreadcrumbs {
    public init() {}

    public func setScreen(_ screen: String) {
        Crashlytics.crashlytics().setCustomValue(screen, forKey: CrashBreadcrumbKey.screen)
        Crashlytics.crashlytics().log("screen: \(screen)")
    }

    public func log(_ message: String) {
        Crashlytics.crashlytics().log(message)
    }
}
