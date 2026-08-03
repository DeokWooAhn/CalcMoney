import Domain

/// Data 레이어의 골격 진입점.
///
/// 0단계에서는 패키지 의존 관계(Data → Domain)만 검증하고,
/// 3단계에서 Firestore·SwiftData 기반 Repository 구현이 추가된다.
public enum DataModule {
    /// 기본 테마 모드 — Domain 의존이 올바르게 연결됐는지 확인하는 용도.
    public static let defaultThemeMode: ThemeMode = .system
}
