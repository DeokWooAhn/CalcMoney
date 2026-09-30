import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("CalculatorViewModel — 통화 선택 직렬화", .timeLimit(.minutes(1)))
struct CalculatorViewModelCurrencyTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let eur = CurrencyInfo(code: "EUR", displayCode: "EUR", name: "유로", flagEmoji: "🇪🇺")
    private static let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "엔", flagEmoji: "🇯🇵")

    private static func makeViewModel(recorder: CalculatorTestRecorder) -> CalculatorViewModel {
        CalculatorTestEnvironment.makeViewModel(
            history: FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(recorder: recorder, currencies: [krw, usd, eur, jpy]),
            currencySelection: FakeCurrencySelectionRepository(
                recorder: recorder,
                // 저장된 선택을 고정해 초기 통화가 기기 로케일에 좌우되지 않게 한다.
                savedSelection: CalculatorCurrencySelection(mainCode: "KRW", subCode: "JPY"),
                // USD 저장이 EUR 저장보다 늦게 끝나도록 해서 저장 완료 순서를 탭 순서와 뒤집는다.
                saveDelays: ["USD": .milliseconds(60), "EUR": .milliseconds(5)],
            ),
        )
    }

    /// 초기 로드는 통화를 정한 뒤 환율을 조회해 채운다. 환율이 채워지면 초기 로드가 끝난 것이다.
    private static func waitForInitialLoad(_ viewModel: CalculatorViewModel) async {
        await waitUntil { viewModel.state.exchangeRate != 0 }
    }

    /// 통화 선택 두 번의 처리가 모두 끝날 때까지 기다린다.
    ///
    /// 초기 로드와 선택 두 번이 환율을 한 번씩 조회하므로 조회가 3건이 되면 마지막 조회까지 시작된 것이다.
    /// 선택 처리는 환율을 0으로 비운 뒤 조회하므로, 환율이 다시 채워지면 마지막 조회 결과까지 반영된 것이다.
    /// 기대하는 결과(EUR)가 화면에 나타나기를 기다리지 않는 이유: 직렬화가 깨지면 EUR이 잠깐 반영됐다가
    /// 늦게 끝난 USD 처리가 덮어쓰는데, 그 사이에 검증하면 버그를 놓친다.
    private static func waitForCurrencySelections(
        _ viewModel: CalculatorViewModel,
        recorder: CalculatorTestRecorder,
    ) async {
        await recorder.waitUntil { $0.requestedRatePairs.count >= 3 }
        await waitUntil { viewModel.state.exchangeRate != 0 }
    }

    @Test("메인 통화를 빠르게 두 번 바꾸면 마지막에 고른 통화가 남는다")
    func 통화_연속_변경은_마지막_선택이_남는다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        await Self.waitForInitialLoad(viewModel)
        #expect(viewModel.state.mainExchangeCurrency?.code == "KRW")

        // 첫 저장이 끝나기 전에 다른 통화를 고르는 상황.
        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        viewModel.send(.selectMainExchangeCurrency(Self.eur))

        await Self.waitForCurrencySelections(viewModel, recorder: recorder)

        #expect(viewModel.state.mainExchangeCurrency?.code == "EUR")
    }

    @Test("통화 저장은 탭한 순서대로 기록된다")
    func 통화_저장은_탭_순서를_따른다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        await Self.waitForInitialLoad(viewModel)

        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        viewModel.send(.selectMainExchangeCurrency(Self.eur))

        await Self.waitForCurrencySelections(viewModel, recorder: recorder)

        // 직렬화가 없으면 저장이 빨리 끝나는 EUR 이 먼저 기록된다.
        let saved = await recorder.savedMainCurrencyCodes
        #expect(saved == ["USD", "EUR"])
    }

    @Test("환율은 마지막에 고른 통화쌍으로 조회된다")
    func 환율은_마지막_통화쌍으로_조회된다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        await Self.waitForInitialLoad(viewModel)

        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        viewModel.send(.selectMainExchangeCurrency(Self.eur))

        await Self.waitForCurrencySelections(viewModel, recorder: recorder)

        // 화면에 남은 통화와 마지막으로 조회한 환율의 통화쌍이 어긋나면 안 된다.
        let pairs = await recorder.requestedRatePairs
        #expect(pairs.last == "EUR->JPY")
        #expect(viewModel.state.mainExchangeCurrency?.code == "EUR")
    }
}
