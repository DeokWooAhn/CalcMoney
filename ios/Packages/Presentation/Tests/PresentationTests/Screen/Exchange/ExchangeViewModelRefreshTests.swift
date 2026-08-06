import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("ExchangeViewModel — 새로고침")
struct ExchangeViewModelRefreshTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")

    private static func makeViewModel(recorder: CalculatorTestRecorder) -> ExchangeViewModel {
        let exchangeRepository = FakeExchangeRateRepository(
            recorder: recorder,
            currencies: [krw, usd],
            rateDate: "2026-08-06",
            // 새로고침이 즉시 끝나지 않게 해서 완료를 기다리는지 확인한다.
            refreshDelay: .milliseconds(80),
        )
        let favoriteRepository = FakeFavoriteCurrencyRepository()
        let selectionRepository = FakeCurrencySelectionRepository(recorder: recorder)

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

    /// `.refreshable`은 클로저가 반환될 때까지 새로고침 표시를 유지한다.
    /// 그래서 완료를 기다릴 수 있는 진입점이 필요하다. `send(_:)`는 Task만 띄우고 곧바로 반환한다.
    @Test("refreshExchangeRates는 새로고침이 끝난 뒤에 반환한다")
    func 새로고침은_완료를_기다린_뒤_반환한다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        try await Task.sleep(for: .milliseconds(200))

        await viewModel.refreshExchangeRates()

        // 추가로 기다리지 않고 곧바로 확인한다. 완료를 기다리지 않았다면 아직 반영되지 않았을 값들이다.
        let refreshCount = await recorder.refreshCount
        #expect(refreshCount == 1)
        #expect(viewModel.state.exchangeRateDate == "2026-08-06")
        #expect(viewModel.state.isLoading == false)
    }

    @Test("send로 보낸 새로고침은 완료를 기다리지 않는다")
    func send는_완료를_기다리지_않는다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        try await Task.sleep(for: .milliseconds(200))

        viewModel.send(.refreshExchangeRates)

        // send가 곧바로 반환한다는 사실 자체를 못박아 둔다.
        // .refreshable에 이걸 쓰면 새로고침 표시가 즉시 사라지는 이유다.
        let refreshCountRightAfterSend = await recorder.refreshCount
        #expect(refreshCountRightAfterSend == 0)

        try await Task.sleep(for: .milliseconds(300))

        let refreshCountAfterWaiting = await recorder.refreshCount
        #expect(refreshCountAfterWaiting == 1)
    }

    @Test("새로고침이 끝나면 로딩 표시가 해제된다")
    func 새로고침_후_로딩이_해제된다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        try await Task.sleep(for: .milliseconds(200))

        await viewModel.refreshExchangeRates()

        #expect(viewModel.state.isLoading == false)
    }
}
