import Domain

public struct ThemeRepositoryImpl: ThemeRepository {
    private let dataSource: ThemePreferenceDataSource

    public init(dataSource: ThemePreferenceDataSource) {
        self.dataSource = dataSource
    }

    public func themeMode() -> AsyncStream<ThemeMode> {
        dataSource.themeMode()
    }

    public func saveThemeMode(_ themeMode: ThemeMode) async throws {
        await dataSource.saveThemeMode(themeMode)
    }
}
