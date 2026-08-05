import Domain
import Foundation

/// 테마 모드를 UserDefaults에 저장하는 데이터소스 (Android `ThemePreferenceDataSource` 대응)
public actor ThemePreferenceDataSource {
    private static let themeModeKey = "theme_mode"

    private let userDefaults: UserDefaults
    private var continuations: [UUID: AsyncStream<ThemeMode>.Continuation] = [:]

    public init(userDefaults: UserDefaults = .suite(named: "theme_preferences")) {
        self.userDefaults = userDefaults
    }

    public nonisolated func themeMode() -> AsyncStream<ThemeMode> {
        AsyncStream { continuation in
            let id = UUID()

            Task { await self.register(id: id, continuation: continuation) }

            continuation.onTermination = { _ in
                Task { await self.unregister(id: id) }
            }
        }
    }

    public func saveThemeMode(_ themeMode: ThemeMode) {
        userDefaults.set(themeMode.rawValue, forKey: Self.themeModeKey)
        broadcast()
    }

    private func currentThemeMode() -> ThemeMode {
        userDefaults.string(forKey: Self.themeModeKey)
            .flatMap(ThemeMode.init(rawValue:)) ?? .system
    }

    private func register(id: UUID, continuation: AsyncStream<ThemeMode>.Continuation) {
        continuations[id] = continuation
        continuation.yield(currentThemeMode())
    }

    private func unregister(id: UUID) {
        continuations[id] = nil
    }

    private func broadcast() {
        let themeMode = currentThemeMode()

        for continuation in continuations.values {
            continuation.yield(themeMode)
        }
    }
}
