import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("CalculatorViewModel — 통화 선택 직렬화")
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

    @Test("메인 통화를 빠르게 두 번 바꾸면 마지막에 고른 통화가 남는다")
    func 통화_연속_변경은_마지막_선택이_남는다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        try await Task.sleep(for: .milliseconds(150))
        #expect(viewModel.state.mainExchangeCurrency?.code == "KRW")

        // 첫 저장이 끝나기 전에 다른 통화를 고르는 상황.
        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        viewModel.send(.selectMainExchangeCurrency(Self.eur))

        try await Task.sleep(for: .milliseconds(500))

        #expect(viewModel.state.mainExchangeCurrency?.code == "EUR")
    }

    @Test("통화 저장은 탭한 순서대로 기록된다")
    func 통화_저장은_탭_순서를_따른다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        try await Task.sleep(for: .milliseconds(150))

        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        viewModel.send(.selectMainExchangeCurrency(Self.eur))

        try await Task.sleep(for: .milliseconds(500))

        // 직렬화가 없으면 저장이 빨리 끝나는 EUR 이 먼저 기록된다.
        let saved = await recorder.savedMainCurrencyCodes
        #expect(saved == ["USD", "EUR"])
    }

    @Test("환율은 마지막에 고른 통화쌍으로 조회된다")
    func 환율은_마지막_통화쌍으로_조회된다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        try await Task.sleep(for: .milliseconds(150))

        viewModel.send(.selectMainExchangeCurrency(Self.usd))
        viewModel.send(.selectMainExchangeCurrency(Self.eur))

        try await Task.sleep(for: .milliseconds(500))

        // 화면에 남은 통화와 마지막으로 조회한 환율의 통화쌍이 어긋나면 안 된다.
        let pairs = await recorder.requestedRatePairs
        #expect(pairs.last == "EUR->JPY")
        #expect(viewModel.state.mainExchangeCurrency?.code == "EUR")
    }
}
