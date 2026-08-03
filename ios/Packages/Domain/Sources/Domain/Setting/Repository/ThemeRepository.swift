public protocol ThemeRepository: Sendable {
    func themeMode() -> AsyncStream<ThemeMode>

    func saveThemeMode(_ themeMode: ThemeMode) async throws
}
