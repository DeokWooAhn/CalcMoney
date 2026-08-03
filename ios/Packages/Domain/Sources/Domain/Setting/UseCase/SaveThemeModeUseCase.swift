public struct SaveThemeModeUseCase: Sendable {
    private let repository: any ThemeRepository

    public init(repository: any ThemeRepository) {
        self.repository = repository
    }

    public func callAsFunction(_ themeMode: ThemeMode) async throws {
        try await repository.saveThemeMode(themeMode)
    }
}
