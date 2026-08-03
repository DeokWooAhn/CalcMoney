public struct GetThemeModeUseCase: Sendable {
    private let repository: any ThemeRepository

    public init(repository: any ThemeRepository) {
        self.repository = repository
    }

    public func callAsFunction() -> AsyncStream<ThemeMode> {
        repository.themeMode()
    }
}
