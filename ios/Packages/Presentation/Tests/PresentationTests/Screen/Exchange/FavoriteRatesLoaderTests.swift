import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("FavoriteRatesLoader — 즐겨찾기 환율 조회와 카드", .timeLimit(.minutes(1)))
struct FavoriteRatesLoaderTests {
    private static let aud = CurrencyInfo(code: "AUD", displayCode: "AUD", name: "호주 달러", flagEmoji: "🇦🇺")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "엔", flagEmoji: "🇯🇵")
    private static let currencies = [aud, usd, krw, jpy]

    private static func makeLoader(
        recorder: CalculatorTestRecorder,
        rateScript: RateScript = RateScript(),
    ) -> FavoriteRatesLoader {
        let exchangeRepository = FakeExchangeRateRepository(recorder: recorder, rateScript: rateScript)
        let favoriteRepository = FakeFavoriteCurrencyRepository()

        return FavoriteRatesLoader(
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

    private static func input(
        from base: CurrencyInfo?,
        favorites: [String],
        currencies: [CurrencyInfo] = currencies,
    ) -> FavoriteRateInput {
        FavoriteRateInput(fromCurrency: base, favoriteCurrencyCodes: favorites, availableCurrencies: currencies)
    }

    /// 조회가 필요 없는 입력이면 실제로 부르지 않는지까지 본다.
    /// 그런 입력은 `needsRates`가 거르므로 ViewModel은 `loadRates`를 부르지 않는다.
    @Test("기준 통화가 없으면 조회할 필요가 없고, 빈 목록 상태를 만들며 환율을 조회하지 않는다")
    func 기준_통화가_없으면_조회하지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(recorder: recorder)
        let input = Self.input(from: nil, favorites: ["USD", "KRW"])

        #expect(loader.needsRates(input) == false)
        #expect(loader.emptyRates(input) == FavoriteRates())
        let rates = await loader.loadRates(input)
        #expect(rates.isEmpty)
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs.isEmpty)
    }

    @Test("통화 목록이 비어 있으면 조회할 필요가 없다")
    func 통화_목록이_비면_조회할_필요가_없다() {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(recorder: recorder)

        #expect(loader.needsRates(Self.input(from: Self.usd, favorites: ["KRW"], currencies: [])) == false)
    }

    @Test("즐겨찾기 통화의 환율을 가져오면 환산 금액과 환율 라벨을 즐겨찾기 순서대로 만든다")
    func 환산_금액과_라벨을_즐겨찾기_순서대로_만든다() async {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(
            recorder: recorder,
            rateScript: RateScript([
                "USD->KRW": [RateStep(rate: 1300)],
                "USD->JPY": [RateStep(rate: 150.5)],
            ]),
        )
        let input = Self.input(from: Self.usd, favorites: ["KRW", "JPY"])

        #expect(loader.needsRates(input))
        let rates = await loader.loadRates(input)
        let items = loader.buildItems(input: input, baseAmount: "2", ratesByCode: rates)

        #expect(items.map(\.currency.code) == ["KRW", "JPY"])
        #expect(items.map(\.convertedAmount) == ["2600.00", "301.00"])
        #expect(items.map(\.rateLabel) == ["1 USD = 1300.0000 KRW", "1 USD = 150.5000 JPY"])
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs == ["USD->KRW", "USD->JPY"])
    }

    @Test("즐겨찾기에 기준 통화, 없는 통화, 실패한 통화가 섞여 있으면 조회에 성공한 통화만 카드로 만든다")
    func 조회에_성공한_통화만_카드로_만든다() async {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(
            recorder: recorder,
            rateScript: RateScript([
                "USD->KRW": [RateStep(error: ExchangeRateError.temporarilyUnavailable())],
                "USD->JPY": [RateStep(rate: 150)],
            ]),
        )
        let input = Self.input(from: Self.usd, favorites: ["USD", "EUR", "KRW", "JPY"])

        let rates = await loader.loadRates(input)
        let items = loader.buildItems(input: input, baseAmount: "1", ratesByCode: rates)

        #expect(rates == ["JPY": 150])
        #expect(items.map(\.currency.code) == ["JPY"])
        // 기준 통화(USD)와 목록에 없는 통화(EUR)는 조회하지 않는다.
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs == ["USD->KRW", "USD->JPY"])
    }

    /// 옛 즐겨찾기 ViewModel 테스트의 "기준 통화만 즐겨찾기되면 안내 상태가 되고 환율을 조회하지 않는다" 중 판단 부분.
    @Test("즐겨찾기가 기준 통화 하나뿐이면 조회할 필요가 없고, 로드 실패가 아니라 기준 통화만 즐겨찾기된 상태를 만든다")
    func 기준_통화만_즐겨찾기되면_안내_상태를_만든다() {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(recorder: recorder)
        let input = Self.input(from: Self.aud, favorites: ["AUD"])

        #expect(loader.needsRates(input) == false)
        let emptyRates = loader.emptyRates(input)
        #expect(emptyRates.baseOnlyFavoriteCode == "AUD")
        #expect(emptyRates.items.isEmpty)
        #expect(emptyRates.isLoading == false)
    }

    /// 옛 즐겨찾기 ViewModel 테스트의 "다른 통화의 조회가 모두 실패하면 안내 상태가 아니다".
    @Test("기준 통화 외의 즐겨찾기가 있지만 환율 조회가 모두 실패하면 조회는 필요하다고 보고, 만들어진 카드는 없다")
    func 다른_통화의_조회가_모두_실패하면_안내_상태가_아니다() async {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(
            recorder: recorder,
            rateScript: RateScript(["AUD->KRW": [RateStep(error: ExchangeRateError.temporarilyUnavailable())]]),
        )
        let input = Self.input(from: Self.aud, favorites: ["AUD", "KRW"])

        // 조회가 필요하다고 보므로 ViewModel은 안내 상태(`emptyRates`)를 만들지 않는다.
        #expect(loader.needsRates(input))
        let rates = await loader.loadRates(input)
        #expect(rates.isEmpty)
        #expect(loader.buildItems(input: input, baseAmount: "1", ratesByCode: rates).isEmpty)
        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs == ["AUD->KRW"])
    }

    /// 옛 즐겨찾기 ViewModel 테스트의 "즐겨찾기가 없으면 안내 상태가 아니다".
    @Test("즐겨찾기가 비어 있으면 조회할 필요가 없고 안내 상태도 만들지 않는다")
    func 즐겨찾기가_없으면_안내_상태가_아니다() {
        let recorder = CalculatorTestRecorder()
        let loader = Self.makeLoader(recorder: recorder)
        let input = Self.input(from: Self.aud, favorites: [])

        #expect(loader.needsRates(input) == false)
        let emptyRates = loader.emptyRates(input)
        #expect(emptyRates.baseOnlyFavoriteCode == nil)
        #expect(emptyRates.items.isEmpty)
    }

    @Test("조회 중에 취소되면 남은 통화는 조회하지 않는다")
    func 취소되면_남은_통화를_조회하지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let gate = TestGate()
        let loader = Self.makeLoader(
            recorder: recorder,
            rateScript: RateScript(["USD->KRW": [RateStep(gate: gate, rate: 1300)]]),
        )
        let input = Self.input(from: Self.usd, favorites: ["KRW", "JPY"])

        let task = Task { await loader.loadRates(input) }
        await recorder.waitUntil { $0.requestedRatePairs.contains("USD->KRW") }
        task.cancel()
        await gate.open()
        _ = await task.value

        let requestedRatePairs = await recorder.requestedRatePairs
        #expect(requestedRatePairs == ["USD->KRW"])
    }
}
