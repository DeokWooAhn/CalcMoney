import Data
import Domain

/// 앱 전체 의존성을 조립하는 수동 DI 컨테이너 (Android의 Hilt 모듈 대응)
///
/// 0단계에서는 골격만 두고, 이후 단계에서
/// Data의 Repository 구현체 → Domain UseCase → ViewModel 순으로 조립이 추가된다.
final class AppContainer {
    /// 앱 기본 테마 모드 — 패키지 연결(Data→Domain) 검증용, 3단계에서 Repository로 대체된다.
    let defaultThemeMode: ThemeMode = DataModule.defaultThemeMode
}
