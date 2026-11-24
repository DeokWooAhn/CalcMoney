import Domain
import Foundation
import Testing
@testable import Presentation

/// 앱을 켜기 전부터 즐겨찾기가 있던 상황을 만든다. 즐겨찾기 흐름이 이 목록을 한 번 내보내고 끝난다.
///
/// 공용 `FakeFavoriteCurrencyRepository`는 아무것도 내보내지 않는다. 그 파일(`CalculatorTestDoubles.swift`)이
/// SwiftLint `file_length` 한도에 닿아 있어 여기에 둔다.
private struct StoredFavoriteCurrencyRepository: FavoriteCurrencyRepository {
    let codes: [String]

    func favoriteCurrencyCodes() -> AsyncStream<[String]> {
        let codes = codes
        return AsyncStream { continuation in
            continuation.yield(codes)
            continuation.finish()
        }
    }

    func addFavorite(_: String) async throws {}

    func removeFavorite(_: String) async throws {}

    func isFavorite(_: String) async throws -> Bool {
        false
    }
}

@MainActor
@Suite("ExchangeViewModel — 새로고침", .timeLimit(.minutes(1)))
struct ExchangeViewModelRefreshTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        refreshGate: TestGate? = nil,
        currenciesError: (any Error)? = nil,
        favoriteCurrencyCodes: [String]? = nil,
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
        let favoriteRepository: any FavoriteCurrencyRepository = favoriteCurrencyCodes
            .map { StoredFavoriteCurrencyRepository(codes: $0) } ?? FakeFavoriteCurrencyRepository()
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
        // 기준 통화(USD)가 아닌 KRW를 즐겨찾기한 채로 시작한다.
        // 시작 로드가 환율과 즐겨찾기(KRW) 환율을 한 번씩, 새로고침이 환율을 한 번 조회하고 모두 실패한다.
        // 그래서 즐겨찾기 카드 없이 실패 분기에 들어간다. 네 번째 조회부터는 1.0으로 성공하므로,
        // 즐겨찾기 조회가 새로고침 중에 다시 돌면 카드가 생기거나 조회 중 표시가 켜져 드러난다.
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            refreshGate: refreshGate,
            favoriteCurrencyCodes: ["KRW"],
            rateScript: RateScript(["USD->KRW": [RateStep](repeating: RateStep(error: unavailable), count: 3)]),
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
        // KRW를 즐겨찾기한 채로 시작한다. 통화 목록이 비어 즐겨찾기 환율은 조회할 필요가 없고,
        // 카드 없이 실패 분기에 들어간다.
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            refreshGate: refreshGate,
            currenciesError: ExchangeRateError.temporarilyUnavailable(),
            favoriteCurrencyCodes: ["KRW"],
        )

        await Self.assertRetryKeepsFailure(
            viewModel: viewModel,
            recorder: recorder,
            refreshGate: refreshGate,
            scope: .currencies,
        )
    }

    /// 즐겨찾기 화면(`FavoriteView`)은 카드 → 즐겨찾기 조회 중(`favoriteRates.isLoading`) → 즐겨찾기 없음
    /// → 기준 통화뿐 → 실패 순으로 분기한다. 그래서 실패 분기(다시 시도 버튼)에 들어가려면 기준 통화가 아닌
    /// 즐겨찾기가 있고, 카드가 없고, 즐겨찾기 조회가 돌고 있지 않아야 한다.
    /// 즐겨찾기가 비어 있으면 앞선 "즐겨찾기한 통화가 없습니다" 분기에 들어가 즐겨찾기 조회가 다시 돌아도 드러나지 않는다.
    ///
    /// 실패 분기는 `FavoriteRateInput`이 바뀌지 않아야 새로고침 내내 유지된다.
    /// 바뀌면 `ExchangeViewModel`이 즐겨찾기 환율을 다시 모으면서 `favoriteRates.isLoading` 분기가 먼저 걸려
    /// 다시 시도 버튼과 그 진행 표시가 사라진다.
    /// 새로고침이 실패로 끝나도 실패 표시는 남고, 결과는 스낵바로만 알린다.
    private static func assertRetryKeepsFailure(
        viewModel: ExchangeViewModel,
        recorder: CalculatorTestRecorder,
        refreshGate: TestGate,
        scope: ExchangeFailure.Scope,
    ) async {
        // 즐겨찾기 조회는 환율 로딩 표시(`isLoading`)와 따로 돌므로, 시작 때의 즐겨찾기 조회가 끝날 때까지 함께 기다린다.
        #expect(await waitUntil {
            viewModel.state.failure != nil && !viewModel.state.isLoading
                && !viewModel.state.favoriteCurrencyCodes.isEmpty && !viewModel.state.favoriteRates.isLoading
        })
        #expect(viewModel.state.failure?.scope == scope)
        expectFavoriteFailureBranch(viewModel, moment: "다시 시도 전")
        let inputBeforeRetry = FavoriteRateInput(viewModel.state)

        viewModel.send(.refreshExchangeRates)
        #expect(await waitUntil { viewModel.state.isLoading })

        // 새로고침이 문 앞에 멈춰 있는 동안. 다시 시도 버튼이 진행 표시를 보여야 하는 구간이다.
        #expect(viewModel.state.failure?.scope == scope)
        #expect(FavoriteRateInput(viewModel.state) == inputBeforeRetry)
        expectFavoriteFailureBranch(viewModel, moment: "새로고침이 문 앞에 멈춘 동안")

        await refreshGate.open()
        #expect(await waitUntil { !viewModel.state.isLoading })

        let refreshCount = await recorder.refreshCount
        #expect(refreshCount == 1)
        #expect(viewModel.state.failure?.scope == scope)
        #expect(FavoriteRateInput(viewModel.state) == inputBeforeRetry)
        expectFavoriteFailureBranch(viewModel, moment: "새로고침이 실패로 끝난 뒤")
    }

    /// 즐겨찾기 화면이 실패 분기 앞의 분기(카드, 조회 중, 즐겨찾기 없음, 기준 통화뿐)에 걸리지 않는지 본다.
    /// `failure`는 호출하는 쪽이 따로 확인한다.
    private static func expectFavoriteFailureBranch(_ viewModel: ExchangeViewModel, moment: Comment) {
        let state = viewModel.state
        #expect(state.favoriteRates.isLoading == false, moment)
        #expect(state.favoriteRates.items.isEmpty, moment)
        #expect(!state.favoriteCurrencyCodes.isEmpty, moment)
        #expect(state.favoriteRates.baseOnlyFavoriteCode == nil, moment)
    }
}
