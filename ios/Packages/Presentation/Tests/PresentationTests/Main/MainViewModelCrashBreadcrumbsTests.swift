import Domain
import Foundation
import os
import Testing
@testable import Presentation

/// 테마 저장소는 이 테스트에서 쓰지 않으므로 아무것도 방출하지 않는다.
private struct IdleThemeRepository: ThemeRepository {
    func themeMode() -> AsyncStream<ThemeMode> {
        AsyncStream { $0.finish() }
    }

    func saveThemeMode(_: ThemeMode) async throws {}
}

/// 남긴 크래시 단서를 기록하는 대역. 프로토콜이 격리되지 않았으므로 `nonisolated`로 둔다.
private final nonisolated class RecordingCrashBreadcrumbs: CrashBreadcrumbs {
    private let state = OSAllocatedUnfairLock(initialState: (screens: [String](), logs: [String]()))

    var screens: [String] { state.withLock { $0.screens } }
    var logs: [String] { state.withLock { $0.logs } }

    func setScreen(_ screen: String) {
        state.withLock { $0.screens.append(screen) }
    }

    func log(_ message: String) {
        state.withLock { $0.logs.append(message) }
    }
}

@MainActor
@Suite("MainViewModel — 크래시 단서")
struct MainViewModelCrashBreadcrumbsTests {
    private let breadcrumbs = RecordingCrashBreadcrumbs()

    private func makeViewModel() -> MainViewModel {
        let theme = IdleThemeRepository()
        let reviewPrompt = FakeReviewPromptRepository()
        let interstitialAd = FakeInterstitialAdRepository(record: InterstitialAdRecord())

        return MainViewModel(
            themeUseCases: ThemeUseCases(
                getThemeMode: GetThemeModeUseCase(repository: theme),
                saveThemeMode: SaveThemeModeUseCase(repository: theme),
            ),
            reviewPromptUseCases: ReviewPromptUseCases(
                recordAppOpen: RecordAppOpenUseCase(repository: reviewPrompt),
                recordCalculationSuccess: RecordCalculationSuccessUseCase(repository: reviewPrompt),
            ),
            interstitialAdUseCases: InterstitialAdUseCases(
                recordCalculation: RecordInterstitialAdCalculationUseCase(repository: interstitialAd),
                shouldShow: ShouldShowInterstitialAdUseCase(repository: interstitialAd),
                recordShown: RecordInterstitialAdShownUseCase(repository: interstitialAd),
            ),
            crashBreadcrumbs: breadcrumbs,
        )
    }

    @Test("하단 탭이 바뀌면 그 탭을 화면 단서로 남긴다")
    func 탭이_바뀌면_화면_단서를_남긴다() {
        makeViewModel().recordScreen(.exchange)

        #expect(breadcrumbs.screens == ["exchange"])
    }

    @Test("광고 요청 가능 여부가 같은 값으로 여러 번 오면 처음 한 번만 남긴다")
    func 같은_광고_동의_상태는_한_번만_남긴다() {
        let viewModel = makeViewModel()

        viewModel.recordAdConsent(canRequestAds: false)
        viewModel.recordAdConsent(canRequestAds: false)

        #expect(breadcrumbs.logs == ["ads: canRequestAds=false"])
    }

    @Test("광고 요청 가능 여부가 바뀌면 바뀐 값을 다시 남긴다")
    func 광고_동의_상태가_바뀌면_다시_남긴다() {
        let viewModel = makeViewModel()

        viewModel.recordAdConsent(canRequestAds: false)
        viewModel.recordAdConsent(canRequestAds: true)

        #expect(breadcrumbs.logs == ["ads: canRequestAds=false", "ads: canRequestAds=true"])
    }

    @Test("인앱 리뷰를 요청하면 리뷰 요청을 남긴다")
    func 인앱_리뷰_요청을_남긴다() {
        makeViewModel().markReviewRequested()

        #expect(breadcrumbs.logs == ["review: in-app review requested"])
    }

    @Test("전면 광고를 보여 줬으면 광고 표시를 남긴다")
    func 전면_광고_표시를_남긴다() {
        makeViewModel().recordInterstitialAdShown()

        #expect(breadcrumbs.logs == ["ads: interstitial shown"])
    }
}
