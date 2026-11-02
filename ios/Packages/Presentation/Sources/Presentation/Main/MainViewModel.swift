import Domain
import Foundation
import Observation
import SwiftUI

/// 앱 전역 테마 상태와 전면 광고를 띄울 차례인지를 관리하는 ViewModel (Android `MainViewModel` 대응)
@MainActor
@Observable
public final class MainViewModel {
    public private(set) var themeMode: ThemeMode = .system

    @ObservationIgnored private let themeUseCases: ThemeUseCases
    @ObservationIgnored private let reviewPromptUseCases: ReviewPromptUseCases
    @ObservationIgnored private let interstitialAdUseCases: InterstitialAdUseCases
    @ObservationIgnored private let crashBreadcrumbs: any CrashBreadcrumbs
    /// 이번 실행에서 인앱 리뷰를 요청했는지. 리뷰를 부탁한 직후에는 전면 광고를 띄우지 않는다.
    @ObservationIgnored private var hasRequestedReviewThisSession = false
    /// 마지막으로 크래시 단서에 남긴 광고 요청 가능 여부. 바뀔 때만 다시 남긴다.
    @ObservationIgnored private var lastRecordedCanRequestAds: Bool?
    @ObservationIgnored private var observeTask: Task<Void, Never>?

    public init(
        themeUseCases: ThemeUseCases,
        reviewPromptUseCases: ReviewPromptUseCases,
        interstitialAdUseCases: InterstitialAdUseCases,
        crashBreadcrumbs: any CrashBreadcrumbs = NoOpCrashBreadcrumbs(),
    ) {
        self.themeUseCases = themeUseCases
        self.reviewPromptUseCases = reviewPromptUseCases
        self.interstitialAdUseCases = interstitialAdUseCases
        self.crashBreadcrumbs = crashBreadcrumbs

        let themeModeStream = themeUseCases.getThemeMode()
        observeTask = Task { [weak self] in
            for await mode in themeModeStream {
                guard let self else { return }

                themeMode = mode
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

    /// 인앱 리뷰 요청 조건에 쓰는 사용 일수를 남긴다. 같은 날 다시 불려도 하루로 센다.
    ///
    /// 앱이 앞으로 올 때마다 부른다. 프로세스가 살아 있는 채로 며칠 뒤 돌아와도 그날을 센다.
    public func recordAppOpen() {
        Task {
            await reviewPromptUseCases.recordAppOpen()
        }
    }

    /// 인앱 리뷰를 요청했다고 남긴다. 이번 실행에서는 전면 광고를 띄우지 않는다.
    public func markReviewRequested() {
        hasRequestedReviewThisSession = true
        crashBreadcrumbs.log("review: in-app review requested")
    }

    /// 지금 보고 있는 하단 탭을 크래시 단서로 남긴다.
    func recordScreen(_ tab: MainTab) {
        crashBreadcrumbs.setScreen(tab.rawValue)
    }

    /// 광고 동의 결과로 광고를 요청할 수 있는지 크래시 단서로 남긴다. 값이 바뀔 때만 남긴다.
    func recordAdConsent(canRequestAds: Bool) {
        if lastRecordedCanRequestAds == canRequestAds { return }

        lastRecordedCanRequestAds = canRequestAds
        crashBreadcrumbs.log("ads: canRequestAds=\(canRequestAds)")
    }

    /// 계산기를 떠나는 지금 전면 광고를 보여 줄 차례인지 알려 준다.
    ///
    /// 광고를 실제로 보여 줬으면 `recordInterstitialAdShown()`을 부른다.
    public func shouldShowInterstitialAd() async -> Bool {
        if hasRequestedReviewThisSession { return false }

        let shouldShow = await interstitialAdUseCases.shouldShow()
        // 기록을 읽는 사이 리뷰를 요청했을 수 있어 다시 확인한다.
        return shouldShow && !hasRequestedReviewThisSession
    }

    public func recordInterstitialAdShown() {
        crashBreadcrumbs.log("ads: interstitial shown")
        Task {
            await interstitialAdUseCases.recordShown()
        }
    }
}

public extension ThemeMode {
    /// SwiftUI `preferredColorScheme` 값. 시스템 설정을 따를 때는 nil.
    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}
