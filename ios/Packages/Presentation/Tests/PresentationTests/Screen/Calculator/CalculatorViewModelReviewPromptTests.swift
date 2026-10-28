import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("CalculatorViewModel — 인앱 리뷰 요청·전면 광고 기록", .timeLimit(.minutes(1)))
struct CalculatorViewModelReviewPromptTests {
    private static let day: TimeInterval = 24 * 60 * 60

    /// 계산 성공 1회만 더하면 요청 조건을 모두 만족하는 기록 (실제 시각 기준)
    private static func almostEligibleRecord() -> ReviewPromptRecord {
        ReviewPromptRecord(
            firstOpenedAt: Date().addingTimeInterval(-10 * day),
            lastOpenedAt: Date(),
            openedDayCount: 3,
            calculationSuccessCount: 4,
        )
    }

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        reviewPrompt: FakeReviewPromptRepository,
    ) -> CalculatorViewModel {
        CalculatorTestEnvironment.makeViewModel(
            history: FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(recorder: recorder),
            currencySelection: FakeCurrencySelectionRepository(recorder: recorder),
            reviewPrompt: reviewPrompt,
        )
    }

    private static func calculateOnePlusTwo(_ viewModel: CalculatorViewModel) async {
        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.number("2")))
        viewModel.send(.calculate)

        await waitUntil { viewModel.state.isCalculatedResult }
    }

    @Test("리뷰 요청 조건을 만족한 상태에서 계산에 성공하면 리뷰 요청 이벤트를 보낸다")
    func 조건을_만족하면_계산_성공_후_리뷰를_요청한다() async {
        let reviewPrompt = FakeReviewPromptRepository(record: Self.almostEligibleRecord())
        let viewModel = Self.makeViewModel(recorder: CalculatorTestRecorder(), reviewPrompt: reviewPrompt)
        var sideEffects = viewModel.sideEffects().makeAsyncIterator()

        await Self.calculateOnePlusTwo(viewModel)

        #expect(await sideEffects.next() == .requestReview)
        #expect(await reviewPrompt.record.lastRequestedAt != nil)
    }

    @Test("리뷰 요청 조건을 만족하지 못하면 계산 성공만 기록하고 요청하지 않는다")
    func 조건_미달이면_계산_성공만_기록한다() async {
        let reviewPrompt = FakeReviewPromptRepository()
        let viewModel = Self.makeViewModel(recorder: CalculatorTestRecorder(), reviewPrompt: reviewPrompt)

        await Self.calculateOnePlusTwo(viewModel)
        // 계산 상태를 반영한 뒤에 기록이 남으므로 기록이 늘 때까지 기다린다.
        await reviewPrompt.waitUntil { $0.calculationSuccessCount >= 1 }

        #expect(await reviewPrompt.record.calculationSuccessCount == 1)
        #expect(await reviewPrompt.record.lastRequestedAt == nil)
    }

    @Test("계산이 실패하면 리뷰 요청 조건에 세지 않는다")
    func 실패한_계산은_세지_않는다() async {
        let reviewPrompt = FakeReviewPromptRepository(record: Self.almostEligibleRecord())
        let viewModel = Self.makeViewModel(recorder: CalculatorTestRecorder(), reviewPrompt: reviewPrompt)
        var sideEffects = viewModel.sideEffects().makeAsyncIterator()

        // 닫히지 않은 괄호는 계산 엔진이 "Error"를 돌려준다.
        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.parenthesis))
        viewModel.send(.calculate)

        // 실패한 계산은 스낵바만 보내고 리뷰 요청 기록은 건드리지 않는다.
        #expect(await sideEffects.next() == .showSnackbar(message: L("계산할 수 없는 수식입니다.")))
        #expect(await reviewPrompt.record.calculationSuccessCount == 4)
    }

    @Test("계산에 성공하면 전면 광고 기록에도 센다")
    func 계산_성공은_전면_광고_기록에도_센다() async {
        let recorder = CalculatorTestRecorder()
        let interstitialAd = FakeInterstitialAdRepository()
        let viewModel = CalculatorTestEnvironment.makeViewModel(
            history: FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(recorder: recorder),
            currencySelection: FakeCurrencySelectionRepository(recorder: recorder),
            interstitialAd: interstitialAd,
        )

        await Self.calculateOnePlusTwo(viewModel)
        // 리뷰 기록 다음에 전면 광고 기록이 남으므로 기록이 늘 때까지 기다린다.
        await interstitialAd.waitUntil { $0.calculationCountSinceLastAd >= 1 }

        let record = await interstitialAd.current
        #expect(record.calculationCountSinceLastAd == 1)
        #expect(record.firstCalculatedAt != nil)
    }
}
