import Domain
import Foundation
import Observation
import Testing
@testable import Presentation

/// 인텐트가 겹칠 때 계산기가 지켜야 할 순서를 본다. Android `CalculatorViewModelOrderingTest`와 같은 시나리오다.
///
/// 시나리오 1(= 연타)은 `CalculatorViewModelCalculateTests`가, 3(통화 저장 순서)은 `CalculatorViewModelCurrencyTests`가 맡는다.
/// 시나리오와 원칙은 `docs/decisions/calculator-intent-ordering.md`에 있다.
/// 순서가 틀린 구현에서 스위트의 1분 제한까지 멈춰 있지 않도록, 결과를 기다리는 곳에는 상한을 둔다.
@MainActor
@Suite("CalculatorViewModel — 인텐트 순서", .timeLimit(.minutes(1)))
struct CalculatorViewModelOrderingTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let eur = CurrencyInfo(code: "EUR", displayCode: "EUR", name: "유로", flagEmoji: "🇪🇺")
    private static let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "엔", flagEmoji: "🇯🇵")

    /// 결과가 나오기를 기다리는 상한. 지연이 없는 정상 구현은 이보다 훨씬 빨리 끝난다.
    private static let waitLimit: Duration = .seconds(5)

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        rateScript: RateScript = RateScript(),
        currenciesGate: TestGate? = nil,
        history: FakeCalculatorHistoryRepository? = nil,
        reviewPrompt: FakeReviewPromptRepository = FakeReviewPromptRepository(),
    ) -> CalculatorViewModel {
        CalculatorTestEnvironment.makeViewModel(
            history: history ?? FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(
                recorder: recorder,
                currencies: [krw, usd, eur, jpy],
                currenciesGate: currenciesGate,
                rateScript: rateScript,
            ),
            currencySelection: FakeCurrencySelectionRepository(
                recorder: recorder,
                // 저장된 선택을 고정해 초기 통화가 기기 로케일에 좌우되지 않게 한다.
                savedSelection: CalculatorCurrencySelection(mainCode: "KRW", subCode: "JPY"),
            ),
            reviewPrompt: reviewPrompt,
        )
    }

    /// 초기 로드는 통화를 정한 뒤 환율을 조회해 채운다. 환율이 채워지면 초기 로드가 끝난 것이다.
    private static func waitForInitialLoad(_ viewModel: CalculatorViewModel) async {
        await waitUntil { viewModel.state.exchangeRate != 0 }
    }

    private static func inputOnePlusTwo(_ viewModel: CalculatorViewModel) {
        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.number("2")))
    }

    /// 첫 USD 조회가 시작된 것을 확인한 뒤 EUR, USD를 고른다. 첫 조회가 진행 중일 때 다음 선택이 들어오게 하려는 것이다.
    private static func selectUsdEurUsd(_ viewModel: CalculatorViewModel, recorder: CalculatorTestRecorder) async {
        viewModel.send(.selectMainExchangeCurrency(usd))
        await recorder.waitUntil { $0.requestedRatePairs.contains("USD->JPY") }

        viewModel.send(.selectMainExchangeCurrency(eur))
        viewModel.send(.selectMainExchangeCurrency(usd))
    }

    /// 초기 로드, USD, EUR, USD 조회 네 건이 응답까지 끝나기를 기다린다.
    ///
    /// 마지막으로 끝나는 조회의 응답을 ViewModel이 처리할 틈을 준 뒤 돌아온다. 지난 응답을 무시하는 구현에서는
    /// 처리 결과가 화면에 드러나지 않아 기다릴 조건이 없으므로 짧게 기다린다. 이 대기가 모자라면 테스트는
    /// 잘못 통과할 수는 있어도 잘못 실패하지는 않는다.
    private static func waitForAllRateResponses(recorder: CalculatorTestRecorder) async {
        await recorder.waitUntil { $0.completedRatePairs.count >= 4 }
        try? await Task.sleep(for: .milliseconds(100))
    }

    @Test("2. = 를 누르고 기록을 지운 뒤 다시 = 를 누르면 추가·삭제·추가 순서로 저장된다")
    func 계산_삭제_계산은_그_순서로_저장된다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)
        await Self.waitForInitialLoad(viewModel)

        Self.inputOnePlusTwo(viewModel)
        viewModel.send(.calculate)
        viewModel.send(.clearHistory)
        viewModel.send(.calculate)

        let isSettled = await waitUntil(within: Self.waitLimit) {
            viewModel.state.expression == "5" && viewModel.state.histories.map(\.expression) == ["3+2"]
        }

        #expect(isSettled)
        #expect(await recorder.savedOperations == ["add:1+2", "clear", "add:3+2"])
    }

    @Test("4-1. USD, EUR, USD 순서로 고르면 첫 USD 응답이 늦게 와도 마지막 요청의 환율만 남는다")
    func 지난_환율_응답은_마지막_요청을_덮지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let rateScript = RateScript([
            "USD->JPY": [RateStep(delay: .milliseconds(300), rate: 1.5), RateStep(rate: 3.0)],
            "EUR->JPY": [RateStep(rate: 2.0)],
        ])
        let viewModel = Self.makeViewModel(recorder: recorder, rateScript: rateScript)
        await Self.waitForInitialLoad(viewModel)

        await Self.selectUsdEurUsd(viewModel, recorder: recorder)
        await Self.waitForAllRateResponses(recorder: recorder)

        #expect(viewModel.state.mainExchangeCurrency?.code == "USD")
        #expect(viewModel.state.exchangeRate == 3.0)
    }

    @Test("4-2. USD, EUR, USD 순서로 고르면 첫 USD 요청이 늦게 실패해도 오류를 띄우지 않는다")
    func 지난_환율_요청의_오류는_띄우지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let rateScript = RateScript([
            "USD->JPY": [
                RateStep(delay: .milliseconds(300), error: ExchangeRateError.networkUnavailable()),
                RateStep(rate: 3.0),
            ],
            "EUR->JPY": [RateStep(rate: 2.0)],
        ])
        let viewModel = Self.makeViewModel(recorder: recorder, rateScript: rateScript)
        let sideEffects = SideEffectLog()
        let collector = Task {
            for await effect in viewModel.sideEffects() {
                sideEffects.effects.append(effect)
            }
        }
        defer { collector.cancel() }
        await Self.waitForInitialLoad(viewModel)

        await Self.selectUsdEurUsd(viewModel, recorder: recorder)
        await Self.waitForAllRateResponses(recorder: recorder)

        #expect(viewModel.state.exchangeRate == 3.0)
        #expect(sideEffects.snackbarMessages.isEmpty)
    }

    @Test("5. 시작 로드가 끝나지 않아도 = 결과가 바로 나온다")
    func 시작_로드를_기다리지_않고_계산한다() async {
        let recorder = CalculatorTestRecorder()
        let currenciesGate = TestGate()
        let viewModel = Self.makeViewModel(recorder: recorder, currenciesGate: currenciesGate)

        Self.inputOnePlusTwo(viewModel)
        viewModel.send(.calculate)

        let isCalculated = await waitUntil(within: Self.waitLimit) { viewModel.state.isCalculatedResult }
        let expression = viewModel.state.expression
        await currenciesGate.open()

        #expect(isCalculated)
        #expect(expression == "3")
    }

    @Test("6. 통화를 바꾼 뒤 환율 조회가 끝나지 않아도 = 결과가 바로 나온다")
    func 환율_조회를_기다리지_않고_계산한다() async {
        let recorder = CalculatorTestRecorder()
        let rateGate = TestGate()
        let rateScript = RateScript(["USD->JPY": [RateStep(gate: rateGate)]])
        let viewModel = Self.makeViewModel(recorder: recorder, rateScript: rateScript)
        await Self.waitForInitialLoad(viewModel)

        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        await recorder.waitUntil { $0.requestedRatePairs.contains("USD->JPY") }

        Self.inputOnePlusTwo(viewModel)
        viewModel.send(.calculate)

        let isCalculated = await waitUntil(within: Self.waitLimit) { viewModel.state.isCalculatedResult }
        let expression = viewModel.state.expression
        await rateGate.open()

        #expect(isCalculated)
        #expect(expression == "3")
    }

    @Test("7. 기록 저장이 실패하면 결과는 보이고 기록 목록에는 없으며 저장 실패 스낵바가 뜬다")
    func 기록_저장이_실패해도_결과는_보인다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            history: FakeCalculatorHistoryRepository(recorder: recorder, saveError: TestFailure.saveFailed),
        )
        let sideEffects = SideEffectLog()
        let collector = Task {
            for await effect in viewModel.sideEffects() {
                sideEffects.effects.append(effect)
            }
        }
        defer { collector.cancel() }
        await Self.waitForInitialLoad(viewModel)

        Self.inputOnePlusTwo(viewModel)
        viewModel.send(.calculate)

        let isReported = await waitUntil(within: Self.waitLimit) {
            sideEffects.snackbarMessages.contains(L("계산 기록을 저장하지 못했습니다."))
        }

        #expect(isReported)
        #expect(viewModel.state.expression == "3")
        #expect(viewModel.state.histories.isEmpty)
    }

    @Test("8. 저장하는 동안 기존 기록이 기록 흐름으로 늦게 도착해도 화면 목록에서 빠지지 않는다")
    func 저장_중_도착한_기존_기록을_덮어쓰지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let reviewPrompt = FakeReviewPromptRepository()
        let earlierHistory = CalculatorHistory(expression: "7×6", result: "42")
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            history: FakeCalculatorHistoryRepository(
                recorder: recorder,
                historyFeed: HistoryFeed(storedBeforeLaunch: [earlierHistory]),
            ),
            reviewPrompt: reviewPrompt,
        )
        await Self.waitForInitialLoad(viewModel)

        Self.inputOnePlusTwo(viewModel)
        viewModel.send(.calculate)
        // 계산 성공은 기록 목록을 고친 다음에 센다. 세어졌으면 기록 목록 처리가 끝난 것이다.
        await reviewPrompt.waitUntil { $0.calculationSuccessCount >= 1 }

        #expect(viewModel.state.histories.map(\.expression) == ["7×6", "1+2"])
    }
}

private enum TestFailure: Error {
    case saveFailed
}

/// 화면이 받은 일회성 이벤트를 모아 둔다. `waitUntil`이 변화를 알아채도록 관찰 가능하게 둔다.
@MainActor
@Observable
private final class SideEffectLog {
    var effects: [CalculatorSideEffect] = []

    var snackbarMessages: [String] {
        effects.compactMap { effect in
            guard case let .showSnackbar(message) = effect else { return nil }
            return message
        }
    }
}

/// 조건을 `limit`까지만 기다린다. 넘으면 `false`를 돌려준다.
///
/// 순서가 틀린 구현에서는 조건이 끝내 참이 되지 않을 수 있다. 그때 스위트의 1분 제한까지 멈춰 있지 않게 한다.
@MainActor
private func waitUntil(within limit: Duration, _ condition: @escaping @MainActor () -> Bool) async -> Bool {
    let waiter = Task { await waitUntil(condition) }
    let timer = Task {
        try? await Task.sleep(for: limit)
        waiter.cancel()
    }
    defer { timer.cancel() }

    return await waiter.value
}
