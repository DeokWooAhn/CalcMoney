import Domain
import Foundation
import Testing
@testable import Presentation

/// 테마 저장소는 이 테스트에서 쓰지 않으므로 아무것도 방출하지 않는다.
private struct IdleThemeRepository: ThemeRepository {
    func themeMode() -> AsyncStream<ThemeMode> {
        AsyncStream { $0.finish() }
    }

    func saveThemeMode(_: ThemeMode) async throws {}
}

@MainActor
@Suite("MainViewModel — 전면 광고", .timeLimit(.minutes(1)))
struct MainViewModelInterstitialAdTests {
    private static let day: TimeInterval = 24 * 60 * 60

    /// 광고 조건을 모두 채운 기록 (실제 시각 기준)
    private static func eligibleRecord() -> InterstitialAdRecord {
        InterstitialAdRecord(
            firstCalculatedAt: Date().addingTimeInterval(-2 * day),
            calculationCountSinceLastAd: 4,
        )
    }

    private static func makeViewModel(interstitialAd: FakeInterstitialAdRepository) -> MainViewModel {
        let theme = IdleThemeRepository()
        let reviewPrompt = FakeReviewPromptRepository()

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
        )
    }

    @Test("광고 조건을 채웠으면 보여 줄 차례라고 알린다")
    func 조건을_채우면_보여_줄_차례다() async {
        let viewModel = Self.makeViewModel(interstitialAd: FakeInterstitialAdRepository(record: Self.eligibleRecord()))

        #expect(await viewModel.shouldShowInterstitialAd())
    }

    @Test("이번 실행에서 인앱 리뷰를 요청했으면 광고 조건과 관계없이 보여 주지 않는다")
    func 리뷰를_요청한_실행에서는_보여_주지_않는다() async {
        let viewModel = Self.makeViewModel(interstitialAd: FakeInterstitialAdRepository(record: Self.eligibleRecord()))

        viewModel.markReviewRequested()

        #expect(await !viewModel.shouldShowInterstitialAd())
    }

    @Test("광고를 보여 줬다고 기록하면 계산 횟수를 다시 세고 다음 광고까지 기다리게 한다")
    func 보여_준_뒤에는_다시_기다린다() async {
        let interstitialAd = FakeInterstitialAdRepository(record: Self.eligibleRecord())
        let viewModel = Self.makeViewModel(interstitialAd: interstitialAd)

        viewModel.recordInterstitialAdShown()
        await interstitialAd.waitUntil { $0.lastShownAt != nil }

        #expect(await interstitialAd.current.calculationCountSinceLastAd == 0)
        #expect(await interstitialAd.current.shownCountOfDay == 1)
        #expect(await !viewModel.shouldShowInterstitialAd())
    }
}
