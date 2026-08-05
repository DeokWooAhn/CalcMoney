import Domain
import Foundation
import Observation
import SwiftUI

/// 앱 전역 테마 상태를 관리하는 ViewModel (Android `MainViewModel` 대응)
@MainActor
@Observable
public final class MainViewModel {
    public private(set) var themeMode: ThemeMode = .system

    @ObservationIgnored private let themeUseCases: ThemeUseCases
    @ObservationIgnored private var observeTask: Task<Void, Never>?

    public init(themeUseCases: ThemeUseCases) {
        self.themeUseCases = themeUseCases

        let themeModeStream = themeUseCases.getThemeMode()
        observeTask = Task { [weak self] in
            for await mode in themeModeStream {
                guard let self else { return }

                self.themeMode = mode
            }
        }
    }

    deinit {
        observeTask?.cancel()
    }

    public func saveThemeMode(_ themeMode: ThemeMode) {
        Task {
            try? await themeUseCases.saveThemeMode(themeMode)
        }
    }
}

extension ThemeMode {
    /// SwiftUI `preferredColorScheme` 값. 시스템 설정을 따를 때는 nil.
    public var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}
