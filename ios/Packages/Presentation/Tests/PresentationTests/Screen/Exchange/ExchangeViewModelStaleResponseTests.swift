import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("ExchangeViewModel — 지난 요청의 응답", .timeLimit(.minutes(1)))
struct ExchangeViewModelStaleResponseTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "엔", flagEmoji: "🇯🇵")

    private static let jpyRate = 9.0

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        rateScript: RateScript,
    ) -> ExchangeViewModel {
        let exchangeRepository = FakeExchangeRateRepository(
            recorder: recorder,
            currencies: [krw, usd, jpy],
            rateDate: "2026-08-06",
            rateScript: rateScript,
        )
        let favoriteRepository = FakeFavoriteCurrencyRepository()
        var selectionRepository = FakeCurrencySelectionRepository(recorder: recorder)
        selectionRepository.savedExchangeSelection = ExchangeCurrencySelection(fromCode: "USD", toCode: "KRW")

        return ExchangeViewModel(
            exchangeUseCases: ExchangeUseCases(
                exchangeAmount: CalculateExchangeAmountUseCase(),
                convertExchangeAmount: ConvertExchangeAmountUseCase(
                    calculateExpression: CalculateExpressionUseCase(),
                ),
                getExchangeRate: GetExchangeRateUseCase(repository: exchangeRepository),
                getLatestRateDate: GetLatestExchangeRateDateUseCase(repository: exchangeRepository),
                getLatestFetchedAt: GetLatestExchangeRateFetchedAtUseCase(repository: exchangeRepository),
                refreshExchangeRates: RefreshExchangeRatesUseCase(repository: exchangeRepository),
                getSupportedCurrencies: GetSupportedCurrenciesUseCase(repository: exchangeRepository),
            ),
            favoriteUseCases: FavoriteUseCases(
                buildFavoriteRates: BuildFavoriteRatesUseCase(),
                getFavoriteCurrencies: GetFavoriteCurrenciesUseCase(repository: favoriteRepository),
                toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase(repository: favoriteRepository),
            ),
            currencySelectionUseCases: CurrencySelectionUseCases(
                getCalculatorSelection: GetCalculatorSelectionUseCase(repository: selectionRepository),
                saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase(repository: selectionRepository),
                saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase(repository: selectionRepository),
                saveCalculatorSelection: SaveCalculatorSelectionUseCase(repository: selectionRepository),
                getExchangeSelection: GetExchangeSelectionUseCase(repository: selectionRepository),
                saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase(repository: selectionRepository),
                saveExchangeToCurrency: SaveExchangeToCurrencyUseCase(repository: selectionRepository),
                saveExchangeSelection: SaveExchangeSelectionUseCase(repository: selectionRepository),
            ),
        )
    }

    @Test("통화를 바꾼 뒤 도착한 지난 요청의 실패는 화면에 남지 않는다")
    func 지난_요청의_실패는_반영되지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let staleGate = TestGate()
        let currentGate = TestGate()
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            rateScript: RateScript([
                "USD->KRW": [RateStep(gate: staleGate, error: ExchangeRateError.temporarilyUnavailable())],
                "USD->JPY": [RateStep(gate: currentGate, rate: Self.jpyRate)],
            ]),
        )

        await recorder.waitUntil { $0.requestedRatePairs.contains("USD->KRW") }

        viewModel.send(.selectToCurrency(Self.jpy))
        await recorder.waitUntil { $0.requestedRatePairs.contains("USD->JPY") }
        #expect(viewModel.state.toCurrency?.code == "JPY")
        #expect(viewModel.state.isLoading == true)

        await staleGate.open()
        await recorder.waitUntil { $0.completedRatePairs.contains("USD->KRW") }
        #expect(await waitUntil { !viewModel.state.isLoading })

        #expect(viewModel.state.failure == nil)

        await currentGate.open()
        #expect(await waitUntil { viewModel.state.exchangeRate == Self.jpyRate })
        #expect(viewModel.state.failure == nil)
    }

    @Test("통화를 바꾸지 않았으면 그 통화쌍의 실패가 화면에 보인다")
    func 요청한_통화쌍_그대로면_실패가_보인다() async {
        let recorder = CalculatorTestRecorder()
        let gate = TestGate()
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            rateScript: RateScript([
                "USD->KRW": [RateStep(gate: gate, error: ExchangeRateError.temporarilyUnavailable())],
            ]),
        )

        await recorder.waitUntil { $0.requestedRatePairs.contains("USD->KRW") }

        await gate.open()
        await recorder.waitUntil { $0.completedRatePairs.contains("USD->KRW") }
        #expect(await waitUntil { viewModel.state.failure != nil })

        #expect(viewModel.state.toCurrency?.code == "KRW")
        #expect(viewModel.state.failure?.scope == .rate)
        #expect(viewModel.state.isLoading == false)
    }
}
