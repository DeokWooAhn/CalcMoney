/// 설정 화면에서 쓰는 테마 유스케이스 묶음 (Android `ThemeUseCases` 대응)
public struct ThemeUseCases: Sendable {
    public let getThemeMode: GetThemeModeUseCase
    public let saveThemeMode: SaveThemeModeUseCase

    public init(getThemeMode: GetThemeModeUseCase, saveThemeMode: SaveThemeModeUseCase) {
        self.getThemeMode = getThemeMode
        self.saveThemeMode = saveThemeMode
    }
}
