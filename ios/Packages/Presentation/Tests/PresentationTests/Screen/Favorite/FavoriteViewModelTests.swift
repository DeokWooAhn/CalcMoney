import Domain
import Foundation
import Testing
@testable import Presentation

/// 통화 쌍별 환율을 돌려주고, 등록되지 않은 쌍은 조회 실패로 처리한다.
private struct StubFavoriteRateRepository: ExchangeRateRepository {
    struct RateNotFound: Error {}

    let recorder: CalculatorTestRecorder
    var rates: [String: Double] = [:]

    func exchangeRate(from: String, to: String) async throws -> Double {
        await recorder.recordRatePair(from: from, to: to)

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
@Suite("FavoriteViewModel — 기준 통화만 즐겨찾기된 경우")
struct FavoriteViewModelTests {
    private static let aud = CurrencyInfo(code: "AUD", displayCode: "AUD", name: "호주 달러", flagEmoji: "🇦🇺")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let currencies = [aud, usd, krw]

    private static func makeViewModel(
        recorder: CalculatorTestRecorder,
        rates: [String: Double] = [:],
    ) -> FavoriteViewModel {
        let exchangeRepository = StubFavoriteRateRepository(recorder: recorder, rates: rates)
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

    @Test("즐겨찾기가 기준 통화 하나뿐이면 로드 실패가 아니라 기준 통화만 즐겨찾기된 상태가 되고 환율을 조회하지 않는다")
    func 기준_통화만_즐겨찾기되면_안내_상태가_되고_환율을_조회하지_않는다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        Self.change(viewModel, from: Self.aud, favorites: ["AUD"])
        viewModel.onBaseAmountChanged("10")
        try await Task.sleep(for: .milliseconds(100))

        #expect(viewModel.state.baseOnlyFavoriteCode == "AUD")
        #expect(viewModel.state.items.isEmpty)
        #expect(viewModel.state.isLoading == false)
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs.isEmpty)
    }

    @Test("기준 통화 외의 즐겨찾기 환율 조회가 모두 실패하면 기준 통화만 즐겨찾기된 상태로 보지 않는다")
    func 다른_통화의_조회가_모두_실패하면_안내_상태가_아니다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        Self.change(viewModel, from: Self.aud, favorites: ["AUD", "KRW"])
        try await Task.sleep(for: .milliseconds(100))

        #expect(viewModel.state.baseOnlyFavoriteCode == nil)
        #expect(viewModel.state.items.isEmpty)
        #expect(viewModel.state.isLoading == false)
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs == ["AUD->KRW"])
    }

    @Test("기준 통화만 즐겨찾기된 상태에서 기준 통화를 바꾸면 이전 기준 통화 카드가 보이고 안내 상태가 풀린다")
    func 기준_통화를_바꾸면_이전_기준_통화_카드가_보인다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder, rates: ["USD->AUD": 1.5])

        Self.change(viewModel, from: Self.aud, favorites: ["AUD"])
        try await Task.sleep(for: .milliseconds(100))
        Self.change(viewModel, from: Self.usd, favorites: ["AUD"])
        try await Task.sleep(for: .milliseconds(100))

        #expect(viewModel.state.baseOnlyFavoriteCode == nil)
        #expect(viewModel.state.items.map(\.currency.code) == ["AUD"])
        #expect(viewModel.state.items.first?.rateLabel == "1 USD = 1.5000 AUD")
    }

    @Test("즐겨찾기가 없으면 기준 통화만 즐겨찾기된 상태로 보지 않는다")
    func 즐겨찾기가_없으면_안내_상태가_아니다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        Self.change(viewModel, from: Self.aud, favorites: [])
        try await Task.sleep(for: .milliseconds(50))

        #expect(viewModel.state.baseOnlyFavoriteCode == nil)
        #expect(viewModel.state.items.isEmpty)
    }
}
