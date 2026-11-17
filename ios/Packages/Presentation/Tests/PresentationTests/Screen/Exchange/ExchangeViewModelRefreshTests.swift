import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("ExchangeViewModel — 새로고침", .timeLimit(.minutes(1)))
struct ExchangeViewModelRefreshTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        refreshGate: TestGate? = nil,
        currenciesError: (any Error)? = nil,
        rateScript: RateScript = RateScript(),
    ) -> ExchangeViewModel {
        let exchangeRepository = FakeExchangeRateRepository(
            recorder: recorder,
            currencies: [krw, usd],
            rateDate: "2026-08-06",
            // 새로고침이 즉시 끝나지 않게 해서 완료를 기다리는지 확인한다.
            refreshDelay: .milliseconds(80),
            refreshGate: refreshGate,
            currenciesError: currenciesError,
            rateScript: rateScript,
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

    /// 초기 로드는 환율 기준일을 채우고 같은 MainActor 실행 안에서 로딩 표시를 끈다.
    /// 기준일은 비어 있다가 한 번 채워지면 그대로이므로, 로딩 표시만 보는 것과 달리 초기 로드가 시작되기 전과 헷갈리지 않는다.
    private static func waitForInitialLoad(_ viewModel: ExchangeViewModel) async {
        await waitUntil { viewModel.state.exchangeRateDate == "2026-08-06" && !viewModel.state.isLoading }
    }

    /// `.refreshable`은 클로저가 반환될 때까지 새로고침 표시를 유지한다.
    /// 그래서 완료를 기다릴 수 있는 진입점이 필요하다. `send(_:)`는 Task만 띄우고 곧바로 반환한다.
    @Test("refreshExchangeRates는 새로고침이 끝난 뒤에 반환한다")
    func 새로고침은_완료를_기다린_뒤_반환한다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        await Self.waitForInitialLoad(viewModel)

        await viewModel.refreshExchangeRates()

        // 추가로 기다리지 않고 곧바로 확인한다. 완료를 기다리지 않았다면 아직 반영되지 않았을 값들이다.
        let refreshCount = await recorder.refreshCount
        #expect(refreshCount == 1)
        #expect(viewModel.state.exchangeRateDate == "2026-08-06")
        #expect(viewModel.state.isLoading == false)
    }

    @Test("send로 보낸 새로고침은 완료를 기다리지 않는다")
    func send는_완료를_기다리지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        await Self.waitForInitialLoad(viewModel)

        viewModel.send(.refreshExchangeRates)

        // send가 곧바로 반환한다는 사실 자체를 못박아 둔다.
        // .refreshable에 이걸 쓰면 새로고침 표시가 즉시 사라지는 이유다.
        let refreshCountRightAfterSend = await recorder.refreshCount
        #expect(refreshCountRightAfterSend == 0)

        // 고정 시간 대신 Task로 띄운 새로고침이 끝났다는 기록을 기다린다.
        await recorder.waitUntil { $0.refreshCount >= 1 }

        let refreshCountAfterWaiting = await recorder.refreshCount
        #expect(refreshCountAfterWaiting == 1)
    }

    @Test("새로고침이 끝나면 로딩 표시가 해제된다")
    func 새로고침_후_로딩이_해제된다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        await Self.waitForInitialLoad(viewModel)

        await viewModel.refreshExchangeRates()

        #expect(viewModel.state.isLoading == false)
    }

    // MARK: - 실패 상태에서 다시 시도

    @Test("환율을 못 받은 상태에서 다시 시도하는 동안 실패 표시와 즐겨찾기 입력이 그대로다")
    func 환율_실패_중_다시_시도하는_동안_실패와_즐겨찾기_입력이_그대로다() async {
        let recorder = CalculatorTestRecorder()
        let refreshGate = TestGate()
        let unavailable = ExchangeRateError.temporarilyUnavailable()
        // 시작 로드와 새로고침이 각각 한 번씩 환율을 조회하고 둘 다 실패한다.
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            refreshGate: refreshGate,
            rateScript: RateScript(["USD->KRW": [RateStep(error: unavailable), RateStep(error: unavailable)]]),
        )

        await Self.assertRetryKeepsFailure(
            viewModel: viewModel,
            recorder: recorder,
            refreshGate: refreshGate,
            scope: .rate,
        )
    }

    @Test("통화 목록을 못 받은 상태에서 다시 시도하는 동안 실패 표시와 즐겨찾기 입력이 그대로다")
    func 통화_목록_실패_중_다시_시도하는_동안_실패와_즐겨찾기_입력이_그대로다() async {
        let recorder = CalculatorTestRecorder()
        let refreshGate = TestGate()
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            refreshGate: refreshGate,
            currenciesError: ExchangeRateError.temporarilyUnavailable(),
        )

        await Self.assertRetryKeepsFailure(
            viewModel: viewModel,
            recorder: recorder,
            refreshGate: refreshGate,
            scope: .currencies,
        )
    }

    /// 즐겨찾기 화면의 실패 분기는 `FavoriteRateInputSnapshot`이 바뀌지 않아야 새로고침 내내 유지된다.
    /// 바뀌면 즐겨찾기 다시 계산이 돌면서 `favoriteState.isLoading` 분기가 먼저 걸려
    /// 다시 시도 버튼과 그 진행 표시가 사라진다.
    /// 새로고침이 실패로 끝나도 실패 표시는 남고, 결과는 스낵바로만 알린다.
    private static func assertRetryKeepsFailure(
        viewModel: ExchangeViewModel,
        recorder: CalculatorTestRecorder,
        refreshGate: TestGate,
        scope: ExchangeFailure.Scope,
    ) async {
        #expect(await waitUntil { viewModel.state.failure != nil && !viewModel.state.isLoading })
        #expect(viewModel.state.failure?.scope == scope)
        let inputBeforeRetry = FavoriteRateInputSnapshot(viewModel.state)

        viewModel.send(.refreshExchangeRates)
        #expect(await waitUntil { viewModel.state.isLoading })

        // 새로고침이 문 앞에 멈춰 있는 동안. 다시 시도 버튼이 진행 표시를 보여야 하는 구간이다.
        #expect(viewModel.state.failure?.scope == scope)
        #expect(FavoriteRateInputSnapshot(viewModel.state) == inputBeforeRetry)

        await refreshGate.open()
        #expect(await waitUntil { !viewModel.state.isLoading })

        let refreshCount = await recorder.refreshCount
        #expect(refreshCount == 1)
        #expect(viewModel.state.failure?.scope == scope)
        #expect(FavoriteRateInputSnapshot(viewModel.state) == inputBeforeRetry)
    }
}
