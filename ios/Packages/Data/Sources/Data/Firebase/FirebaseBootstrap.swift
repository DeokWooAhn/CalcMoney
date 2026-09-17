import FirebaseAppCheck
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

        // App Check provider factory는 반드시 configure() 이전에 지정해야 한다.
        // 이후에 설정하면 첫 토큰 요청이 provider 없이 나가서 검증에 실패한다.
        AppCheck.setAppCheckProviderFactory(appCheckProviderFactory)

        FirebaseApp.configure()

        return true
    }

    /// 빌드 구성에 맞는 App Check provider factory.
    ///
    /// App Attest는 시뮬레이터에서 동작하지 않으므로 디버그 빌드는 debug provider를 쓴다.
    /// debug provider가 만든 토큰은 Firebase 콘솔에 등록된 것만 유효하다.
    private static var appCheckProviderFactory: any AppCheckProviderFactory {
        #if DEBUG
            AppCheckDebugProviderFactory()
        #else
            AppAttestProviderFactory()
        #endif
    }
}

#if !DEBUG
    /// 릴리스 빌드용 App Attest provider factory.
    ///
    /// 배포 타깃이 iOS 17 이상이라 App Attest를 항상 쓸 수 있어 DeviceCheck 폴백은 두지 않는다.
    private final class AppAttestProviderFactory: NSObject, AppCheckProviderFactory {
        func createProvider(with app: FirebaseApp) -> (any AppCheckProvider)? {
            AppAttestProvider(app: app)
        }
    }
#endif
