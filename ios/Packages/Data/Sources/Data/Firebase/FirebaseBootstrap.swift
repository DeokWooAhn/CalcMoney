import FirebaseCore
import Foundation

/// Firebase 초기화 진입점.
///
/// Firebase 관련 지식을 Data 패키지 안에 가두기 위해 앱 타깃 대신 여기서 초기화한다.
public enum FirebaseBootstrap {
    /// 번들에 GoogleService-Info.plist가 있으면 Firebase를 초기화하고 true를 반환한다.
    ///
    /// plist가 아직 없는 개발 단계에서도 앱이 크래시 없이 뜨도록 하기 위한 가드.
    @MainActor
    public static func configureIfAvailable() -> Bool {
        // 이미 초기화됐으면(예: 테스트에서 AppContainer를 다시 만들 때) 중복 호출하지 않는다.
        if FirebaseApp.app() != nil {
            return true
        }

        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return false
        }

        FirebaseApp.configure()

        return true
    }
}
