import Domain
import Foundation
import Testing
@testable import Presentation

/// 통화 쌍별 환율을 돌려주고, 등록되지 않은 쌍은 조회 실패로 처리한다.
/// 조회를 하나 끝낼 때마다 `lookups`로 알려서 테스트가 시간 대신 이 신호를 기다리게 한다.
private struct StubFavoriteRateRepository: ExchangeRateRepository {
    struct RateNotFound: Error {}

    let recorder: CalculatorTestRecorder
    let lookups: AsyncStream<Void>.Continuation
    var rates: [String: Double] = [:]

    func exchangeRate(from: String, to: String) async throws -> Double {
        await recorder.recordRatePair(from: from, to: to)
        defer { lookups.yield() }

        guard let rate = rates["\(from)->\(to)"] else { throw RateNotFound() }
        return rate
    }

    func latestRateDate() async throws -> String {
        ""
    }

    func latestFetchedAt() async throws -> Int {
        0
    }

    func refreshExchangeRates() async throws {}

    func supportedCurrencies() async throws -> [CurrencyInfo] {
        []
    }
}

@MainActor
@Suite("FavoriteViewModel — 기준 통화만 즐겨찾기된 경우", .timeLimit(.minutes(1)))
struct FavoriteViewModelTests {
    private static let aud = CurrencyInfo(code: "AUD", displayCode: "AUD", name: "호주 달러", flagEmoji: "🇦🇺")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let currencies = [aud, usd, krw]

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        lookups: AsyncStream<Void>.Continuation = AsyncStream<Void>.makeStream().continuation,
        rates: [String: Double] = [:],
    ) -> FavoriteViewModel {
        let exchangeRepository = StubFavoriteRateRepository(recorder: recorder, lookups: lookups, rates: rates)
        let favoriteRepository = FakeFavoriteCurrencyRepository()

        return FavoriteViewModel(
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
        )
    }

    private static func change(
        _ viewModel: FavoriteViewModel,
        from currency: CurrencyInfo,
        favorites: [String],
    ) {
        viewModel.onExchangeStateChanged(
            fromCurrency: currency,
            favoriteCurrencyCodes: favorites,
            availableCurrencies: currencies,
            exchangeRateDate: "2026-09-29",
            exchangeRateFetchedAt: 1000,
        )
    }

    /// 환율 조회 `count`개가 끝난 뒤 조회 Task가 로딩을 마칠 때까지 기다린다.
    /// 로딩 표시는 조회 전에 켜지므로, 마지막 조회가 끝난 뒤에는 꺼지는 변화만 남는다.
    private static func waitForLoad(
        _ viewModel: FavoriteViewModel,
        lookups: AsyncStream<Void>,
        count: Int,
    ) async {
        var remaining = count
        for await _ in lookups {
            remaining -= 1
            if remaining == 0 { break }
        }
        await waitUntil { !viewModel.state.isLoading }
    }

    @Test("즐겨찾기가 기준 통화 하나뿐이면 로드 실패가 아니라 기준 통화만 즐겨찾기된 상태가 되고 환율을 조회하지 않는다")
    func 기준_통화만_즐겨찾기되면_안내_상태가_되고_환율을_조회하지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        // 조회 Task를 만들지 않고 곧바로 상태를 바꾸는 경로라 기다릴 필요가 없다.
        Self.change(viewModel, from: Self.aud, favorites: ["AUD"])
        viewModel.onBaseAmountChanged("10")

        #expect(viewModel.state.baseOnlyFavoriteCode == "AUD")
        #expect(viewModel.state.items.isEmpty)
        #expect(viewModel.state.isLoading == false)
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs.isEmpty)
    }

    @Test("기준 통화 외의 즐겨찾기 환율 조회가 모두 실패하면 기준 통화만 즐겨찾기된 상태로 보지 않는다")
    func 다른_통화의_조회가_모두_실패하면_안내_상태가_아니다() async {
        let recorder = CalculatorTestRecorder()
        let (lookups, lookupContinuation) = AsyncStream<Void>.makeStream()
        let viewModel = Self.makeViewModel(recorder: recorder, lookups: lookupContinuation)

        Self.change(viewModel, from: Self.aud, favorites: ["AUD", "KRW"])
        await Self.waitForLoad(viewModel, lookups: lookups, count: 1)

        #expect(viewModel.state.baseOnlyFavoriteCode == nil)
        #expect(viewModel.state.items.isEmpty)
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs == ["AUD->KRW"])
    }

    @Test("기준 통화만 즐겨찾기된 상태에서 기준 통화를 바꾸면 이전 기준 통화 카드가 보이고 안내 상태가 풀린다")
    func 기준_통화를_바꾸면_이전_기준_통화_카드가_보인다() async {
        let recorder = CalculatorTestRecorder()
        let (lookups, lookupContinuation) = AsyncStream<Void>.makeStream()
        let viewModel = Self.makeViewModel(
            recorder: recorder,
            lookups: lookupContinuation,
            rates: ["USD->AUD": 1.5],
        )

        Self.change(viewModel, from: Self.aud, favorites: ["AUD"])
        #expect(viewModel.state.baseOnlyFavoriteCode == "AUD")

        Self.change(viewModel, from: Self.usd, favorites: ["AUD"])
        await Self.waitForLoad(viewModel, lookups: lookups, count: 1)

        #expect(viewModel.state.baseOnlyFavoriteCode == nil)
        #expect(viewModel.state.items.map(\.currency.code) == ["AUD"])
        #expect(viewModel.state.items.first?.rateLabel == "1 USD = 1.5000 AUD")
    }

    /// 조회 Task가 시작되기 전에 다음 입력이 와서 취소되면, 늦게 시작한 Task가 새 상태를 덮어쓰면 안 된다.
    @Test("조회 Task가 시작 전에 취소되면 뒤이은 기준 통화 안내 상태를 덮어쓰지 않는다")
    func 시작_전에_취소된_조회는_안내_상태를_덮어쓰지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder, rates: ["AUD->KRW": 900])

        // 두 호출 사이에 중단 지점이 없으므로 첫 조회 Task는 시작도 하기 전에 취소된다.
        Self.change(viewModel, from: Self.aud, favorites: ["AUD", "KRW"])
        Self.change(viewModel, from: Self.aud, favorites: ["AUD"])
        // 취소된 Task가 할 일이 없어야 하므로 기다릴 신호가 없다. MainActor 작업은 들어온 순서대로 실행되므로
        // 뒤에 넣은 빈 작업이 끝났다면 먼저 들어간 취소된 Task도 이미 실행을 마친 것이다.
        await Task { @MainActor in }.value

        #expect(viewModel.state.baseOnlyFavoriteCode == "AUD")
        #expect(viewModel.state.isLoading == false)
        #expect(viewModel.state.items.isEmpty)
    }

    @Test("즐겨찾기가 없으면 기준 통화만 즐겨찾기된 상태로 보지 않는다")
    func 즐겨찾기가_없으면_안내_상태가_아니다() {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        Self.change(viewModel, from: Self.aud, favorites: [])

        #expect(viewModel.state.baseOnlyFavoriteCode == nil)
        #expect(viewModel.state.items.isEmpty)
    }
}
