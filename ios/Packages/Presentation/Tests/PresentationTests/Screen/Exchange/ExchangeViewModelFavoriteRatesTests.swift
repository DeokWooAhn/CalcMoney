import Domain
import Foundation
import Testing
@testable import Presentation

/// `FakeExchangeRateRepository`에 받은 시각만 `setFetchedAt(_:)`으로 바꿀 수 있게 덧씌운다. 서버 문서가 바뀐 상황을 만든다.
private actor FetchedAtExchangeRateRepository: ExchangeRateRepository {
    let base: FakeExchangeRateRepository
    private var fetchedAt: Int

    init(base: FakeExchangeRateRepository, fetchedAt: Int) {
        self.base = base
        self.fetchedAt = fetchedAt
    }

    func setFetchedAt(_ value: Int) {
        fetchedAt = value
    }

    func exchangeRate(from: String, to: String) async throws -> Double {
        try await base.exchangeRate(from: from, to: to)
    }

    func latestRateDate() async throws -> String {
        try await base.latestRateDate()
    }

    func latestFetchedAt() async throws -> Int {
        fetchedAt
    }

    func refreshExchangeRates() async throws {
        try await base.refreshExchangeRates()
    }

    func supportedCurrencies() async throws -> [CurrencyInfo] {
        try await base.supportedCurrencies()
    }
}

/// 즐겨찾기 목록 흐름을 테스트가 `send(_:)`로 직접 내보낸다. 즐겨찾기를 더하거나 지운 상황을 만든다.
private final class FeedFavoriteCurrencyRepository: FavoriteCurrencyRepository, Sendable {
    private let stream: AsyncStream<[String]>
    private let continuation: AsyncStream<[String]>.Continuation

    init() {
        (stream, continuation) = AsyncStream.makeStream()
    }

    func send(_ codes: [String]) {
        continuation.yield(codes)
    }

    func favoriteCurrencyCodes() -> AsyncStream<[String]> {
        stream
    }

    func addFavorite(_: String) async throws {}

    func removeFavorite(_: String) async throws {}

    func isFavorite(_: String) async throws -> Bool {
        false
    }
}

@MainActor
private struct FavoriteRatesFixture {
    static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    static let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "엔", flagEmoji: "🇯🇵")
    static let eur = CurrencyInfo(code: "EUR", displayCode: "EUR", name: "유로", flagEmoji: "🇪🇺")
    static let gbp = CurrencyInfo(code: "GBP", displayCode: "GBP", name: "파운드", flagEmoji: "🇬🇧")
    static let rateDate = "2026-08-06"
    /// "일어나지 않아야 한다"를 검사할 때 기다리는 상한. 응답이 하나 처리되기에 넉넉하다.
    static let settleLimit = Duration.milliseconds(200)

    let recorder = CalculatorTestRecorder()
    let favoriteRepository = FeedFavoriteCurrencyRepository()
    let exchangeRepository: FetchedAtExchangeRateRepository
    let viewModel: ExchangeViewModel

    /// USD→KRW로 시작해 시작 로드가 끝난 뒤 돌려준다(그 전에 즐겨찾기를 내보내면 조회 순서가 흔들린다). 적지 않은 환율은 1.0이다.
    static func started(rateScript: RateScript = RateScript()) async -> Self {
        let fixture = Self(rateScript: rateScript)
        await waitUntil { fixture.viewModel.state.exchangeRateDate == rateDate && !fixture.viewModel.state.isLoading }
        return fixture
    }

    private init(rateScript: RateScript) {
        exchangeRepository = FetchedAtExchangeRateRepository(
            base: FakeExchangeRateRepository(
                recorder: recorder,
                currencies: [Self.krw, Self.usd, Self.jpy, Self.eur, Self.gbp],
                rateDate: Self.rateDate,
                rateScript: rateScript,
            ),
            fetchedAt: 1000,
        )
        var selectionRepository = FakeCurrencySelectionRepository(recorder: recorder)
        selectionRepository.savedExchangeSelection = ExchangeCurrencySelection(fromCode: "USD", toCode: "KRW")

        viewModel = ExchangeViewModel(
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

    var favoriteRates: FavoriteRates { viewModel.state.favoriteRates }
    var labels: [String] { favoriteRates.items.map(\.rateLabel) }
    var amounts: [String] { favoriteRates.items.map(\.convertedAmount) }

    /// 즐겨찾기를 내보내고 ViewModel이 받을 때까지 기다린다. 받는 순간 다시 모을지 판단하므로 돌아오면 조회는 이미 시작됐다.
    func sendFavorites(_ codes: [String]) async {
        favoriteRepository.send(codes)
        await waitUntil { viewModel.state.favoriteCurrencyCodes == codes }
    }

    /// 즐겨찾기를 내보내고 그 조회가 끝날 때까지 기다린다.
    func loadFavorites(_ codes: [String]) async {
        await sendFavorites(codes)
        await waitUntil { !favoriteRates.isLoading }
    }

    func requestCount(_ pair: String) async -> Int {
        await recorder.requestedRatePairs.count(where: { $0 == pair })
    }
}

@MainActor
@Suite("ExchangeViewModel — 즐겨찾기 카드", .timeLimit(.minutes(1)))
struct ExchangeViewModelFavoriteRatesTests {
    private typealias Fixture = FavoriteRatesFixture

    @Test("즐겨찾기 환율을 조회하는 동안 로딩을 켰다가 조회가 끝나면 카드와 함께 끈다")
    func 조회_중_로딩을_켰다가_카드와_함께_끈다() async {
        let gate = TestGate()
        let fixture = await Fixture.started(rateScript: RateScript(["USD->JPY": [RateStep(gate: gate, rate: 9)]]))

        await fixture.sendFavorites(["JPY"])
        await fixture.recorder.waitUntil { $0.requestedRatePairs.contains("USD->JPY") }

        #expect(fixture.favoriteRates.isLoading)
        #expect(fixture.favoriteRates.items.isEmpty)

        await gate.open()
        #expect(await waitUntil { !fixture.favoriteRates.isLoading })

        #expect(fixture.labels == ["1 USD = 9.0000 JPY"])
        #expect(fixture.amounts == ["9.00"])
        #expect(fixture.favoriteRates.baseOnlyFavoriteCode == nil)
    }

    /// 다시 조회하면 로딩이 같은 실행 안에서 켜지고, 두 번째 조회는 1.0을 돌려 라벨이 바뀐다. 둘 다 아니어야 한다.
    @Test("기준 금액만 바뀌면 환율을 다시 조회하지 않고 모은 환율로 카드 금액만 다시 계산한다")
    func 금액만_바뀌면_다시_조회하지_않는다() async {
        let fixture = await Fixture.started(rateScript: RateScript(["USD->JPY": [RateStep(rate: 9)]]))
        await fixture.loadFavorites(["JPY"])

        fixture.viewModel.send(.updateFromAmount("10"))

        #expect(fixture.amounts == ["90.00"])
        #expect(fixture.labels == ["1 USD = 9.0000 JPY"])
        #expect(fixture.favoriteRates.isLoading == false)
        #expect(await fixture.requestCount("USD->JPY") == 1)
    }

    @Test("같은 즐겨찾기 입력으로 상태가 다시 바뀌면(도착 통화 변경으로 로딩이 오가도) 카드를 유지하고 다시 조회하지 않는다")
    func 같은_입력이면_카드를_유지하고_다시_조회하지_않는다() async {
        let fixture = await Fixture.started(rateScript: RateScript([
            "USD->JPY": [RateStep(rate: 9)],
            "USD->EUR": [RateStep(rate: 0.8)],
        ]))
        await fixture.loadFavorites(["JPY"])
        let itemsBefore = fixture.favoriteRates.items

        fixture.viewModel.send(.selectToCurrency(Fixture.eur))
        #expect(await waitUntil { fixture.viewModel.state.exchangeRate == 0.8 && !fixture.viewModel.state.isLoading })

        #expect(fixture.favoriteRates.items == itemsBefore)
        #expect(fixture.favoriteRates.isLoading == false)
        #expect(await fixture.requestCount("USD->JPY") == 1)
    }

    /// 옛 즐겨찾기 ViewModel 테스트의 "기준 통화만 즐겨찾기되면 안내 상태가 되고 환율을 조회하지 않는다".
    @Test("기준 통화만 즐겨찾기된 상태이면 안내 상태를 만들고 환율을 조회하지 않으며, 금액이 바뀌어도 그대로다")
    func 기준_통화만_즐겨찾기되면_안내_상태가_되고_조회하지_않는다() async {
        let fixture = await Fixture.started()

        await fixture.sendFavorites(["USD"])
        fixture.viewModel.send(.updateFromAmount("10"))

        #expect(fixture.favoriteRates.baseOnlyFavoriteCode == "USD")
        #expect(fixture.favoriteRates.items.isEmpty)
        #expect(fixture.favoriteRates.isLoading == false)
        // 시작 로드의 환율 조회 하나뿐이다.
        let requestedRatePairs = await fixture.recorder.requestedRatePairs
        #expect(requestedRatePairs == ["USD->KRW"])
    }

    /// 옛 즐겨찾기 ViewModel 테스트의 "기준 통화를 바꾸면 이전 기준 통화 카드가 보인다".
    @Test("기준 통화만 즐겨찾기된 상태에서 기준 통화를 바꾸면 이전 기준 통화의 카드를 보여 주고 안내 상태를 해제한다")
    func 기준_통화를_바꾸면_이전_기준_통화_카드가_보인다() async {
        let fixture = await Fixture.started(rateScript: RateScript(["EUR->USD": [RateStep(rate: 1.5)]]))
        await fixture.sendFavorites(["USD"])
        #expect(fixture.favoriteRates.baseOnlyFavoriteCode == "USD")

        fixture.viewModel.send(.selectFromCurrency(Fixture.eur))
        #expect(await waitUntil { !fixture.favoriteRates.items.isEmpty && !fixture.favoriteRates.isLoading })

        #expect(fixture.favoriteRates.baseOnlyFavoriteCode == nil)
        #expect(fixture.labels == ["1 EUR = 1.5000 USD"])
    }

    @Test("즐겨찾기 조건이 사라지면 이전 카드를 비우고, 금액이 바뀌어도 이전 환율로 카드를 되살리지 않는다")
    func 조건이_사라지면_카드를_비우고_되살리지_않는다() async {
        let fixture = await Fixture.started(rateScript: RateScript(["USD->JPY": [RateStep(rate: 9)]]))
        await fixture.loadFavorites(["JPY"])

        await fixture.sendFavorites([])
        fixture.viewModel.send(.updateFromAmount("5"))

        #expect(fixture.favoriteRates == FavoriteRates())
    }

    /// 앞 조회를 문으로 붙잡아, 뒤 조회가 먼저 끝나고 앞 조회가 나중에 끝나는 순서를 강제한다.
    @Test("기준 통화를 빠르게 두 번 바꾸면 앞 조회가 늦게 끝나도 뒤 기준 통화의 카드를 덮지 않는다")
    func 앞_조회가_늦게_끝나도_뒤_카드를_덮지_않는다() async {
        let firstGate = TestGate()
        let fixture = await Fixture.started(rateScript: RateScript([
            "USD->JPY": [RateStep(rate: 9)],
            "EUR->JPY": [RateStep(gate: firstGate, rate: 160)],
            "GBP->JPY": [RateStep(rate: 180)],
        ]))
        await fixture.loadFavorites(["JPY"])

        fixture.viewModel.send(.selectFromCurrency(Fixture.eur))
        await fixture.recorder.waitUntil { $0.requestedRatePairs.contains("EUR->JPY") }
        fixture.viewModel.send(.selectFromCurrency(Fixture.gbp))
        #expect(await waitUntil { fixture.labels == ["1 GBP = 180.0000 JPY"] && !fixture.favoriteRates.isLoading })

        await firstGate.open()
        await fixture.recorder.waitUntil { $0.completedRatePairs.contains("EUR->JPY") }
        let overwritten = await waitUntil(
            { fixture.labels != ["1 GBP = 180.0000 JPY"] || fixture.favoriteRates.isLoading },
            within: Fixture.settleLimit,
        )

        #expect(overwritten == false)
    }

    /// 옛 즐겨찾기 ViewModel 테스트의 "시작 전에 취소된 조회는 안내 상태를 덮어쓰지 않는다"를 새 구조에 맞게 옮겼다.
    /// 입력이 바뀌면 앞 조회는 취소되고, 늦게 끝나도 뒤 입력이 만든 상태를 쓰지 않는다.
    @Test("입력이 빠르게 두 번 바뀌면 앞 조회가 늦게 끝나도 뒤이은 기준 통화 안내 상태를 덮지 않는다")
    func 앞_조회는_뒤이은_안내_상태를_덮지_않는다() async {
        let gate = TestGate()
        let fixture = await Fixture.started(rateScript: RateScript(["USD->JPY": [RateStep(gate: gate, rate: 9)]]))

        await fixture.sendFavorites(["JPY"])
        await fixture.recorder.waitUntil { $0.requestedRatePairs.contains("USD->JPY") }
        await fixture.sendFavorites(["USD"])

        #expect(fixture.favoriteRates.baseOnlyFavoriteCode == "USD")
        #expect(fixture.favoriteRates.isLoading == false)

        await gate.open()
        await fixture.recorder.waitUntil { $0.completedRatePairs.contains("USD->JPY") }
        let overwritten = await waitUntil(
            { fixture.favoriteRates.baseOnlyFavoriteCode != "USD" || !fixture.favoriteRates.items.isEmpty },
            within: Fixture.settleLimit,
        )

        #expect(overwritten == false)
        #expect(fixture.favoriteRates.isLoading == false)
    }

    @Test("새로고침이 성공해 환율을 받은 시각이 바뀌면 즐겨찾기 환율을 다시 조회해 카드를 바꾼다")
    func 받은_시각이_바뀌면_다시_조회한다() async {
        let fixture = await Fixture.started(
            rateScript: RateScript(["USD->JPY": [RateStep(rate: 9), RateStep(rate: 9.5)]]),
        )
        await fixture.loadFavorites(["JPY"])
        #expect(fixture.labels == ["1 USD = 9.0000 JPY"])

        await fixture.exchangeRepository.setFetchedAt(2000)
        await fixture.viewModel.refreshExchangeRates()
        #expect(fixture.viewModel.state.exchangeRateFetchedAt == 2000)
        #expect(await waitUntil { fixture.labels == ["1 USD = 9.5000 JPY"] && !fixture.favoriteRates.isLoading })

        #expect(await fixture.requestCount("USD->JPY") == 2)
    }

    @Test("즐겨찾기 조회가 실패한 뒤 같은 서버 문서로 새로고침이 성공하면, 날짜·받은 시각이 그대로여도 다시 조회해 카드를 만든다")
    func 실패_뒤_같은_문서로_새로고침해도_다시_조회한다() async {
        let fixture = await Fixture.started(rateScript: RateScript([
            "USD->JPY": [RateStep(error: ExchangeRateError.temporarilyUnavailable()), RateStep(rate: 9.5)],
        ]))
        await fixture.loadFavorites(["JPY"])
        #expect(fixture.favoriteRates.items.isEmpty)
        #expect(fixture.favoriteRates.baseOnlyFavoriteCode == nil)

        await fixture.viewModel.refreshExchangeRates()
        #expect(fixture.viewModel.state.exchangeRateDate == Fixture.rateDate)
        #expect(fixture.viewModel.state.exchangeRateFetchedAt == 1000)
        #expect(fixture.viewModel.state.ratesRefreshCount == 1)
        #expect(await waitUntil { fixture.labels == ["1 USD = 9.5000 JPY"] && !fixture.favoriteRates.isLoading })
    }

    @Test("즐겨찾기를 더해 재조회가 끝나지 않은 동안 금액을 바꾸면 기존 카드 금액이 바로 바뀌고, 조회가 끝나면 새 즐겨찾기까지 새 금액으로 보인다")
    func 즐겨찾기를_더한_재조회_중_금액을_바꾸면_기존_카드가_바로_따라간다() async {
        let gate = TestGate()
        let fixture = await Fixture.started(rateScript: RateScript([
            "USD->JPY": [RateStep(rate: 9), RateStep(rate: 9)],
            "USD->EUR": [RateStep(gate: gate, rate: 1.2)],
        ]))
        await fixture.loadFavorites(["JPY"])

        await fixture.sendFavorites(["JPY", "EUR"])
        await fixture.recorder.waitUntil { $0.requestedRatePairs.contains("USD->EUR") }
        fixture.viewModel.send(.updateFromAmount("2"))

        #expect(fixture.favoriteRates.items.map(\.currency.code) == ["JPY"])
        #expect(fixture.amounts == ["18.00"])
        #expect(fixture.favoriteRates.isLoading)

        await gate.open()
        #expect(await waitUntil { !fixture.favoriteRates.isLoading })
        #expect(fixture.favoriteRates.items.map(\.currency.code) == ["JPY", "EUR"])
        #expect(fixture.amounts == ["18.00", "2.40"])
    }

    @Test("기준 통화를 바꿔 재조회가 끝나지 않은 동안 금액을 바꾸면 옛 기준 통화의 라벨과 환율로 금액만 바뀌고, 새 기준 통화 라벨에 옛 환율을 섞지 않는다")
    func 기준_통화를_바꾼_재조회_중_금액을_바꾸면_옛_라벨과_환율을_쓴다() async {
        let gate = TestGate()
        let fixture = await Fixture.started(rateScript: RateScript([
            "USD->JPY": [RateStep(rate: 9)],
            "EUR->JPY": [RateStep(gate: gate, rate: 160)],
        ]))
        await fixture.loadFavorites(["JPY"])

        fixture.viewModel.send(.selectFromCurrency(Fixture.eur))
        await fixture.recorder.waitUntil { $0.requestedRatePairs.contains("EUR->JPY") }
        fixture.viewModel.send(.updateFromAmount("2"))

        #expect(fixture.labels == ["1 USD = 9.0000 JPY"])
        #expect(fixture.amounts == ["18.00"])

        await gate.open()
        #expect(await waitUntil { !fixture.favoriteRates.isLoading })
        #expect(fixture.labels == ["1 EUR = 160.0000 JPY"])
        #expect(fixture.amounts == ["320.00"])
    }

    @Test("즐겨찾기를 모두 지운 뒤 다시 더해 재조회가 끝나지 않은 동안 금액을 바꾸면 지운 카드를 되살리지 않고, 조회가 끝나면 새 즐겨찾기 카드만 보인다")
    func 모두_지운_뒤_다시_더한_재조회_중_금액을_바꿔도_지운_카드를_되살리지_않는다() async {
        let gate = TestGate()
        let fixture = await Fixture.started(rateScript: RateScript([
            "USD->JPY": [RateStep(rate: 9)],
            "USD->EUR": [RateStep(gate: gate, rate: 1.2)],
        ]))
        await fixture.loadFavorites(["JPY"])

        await fixture.sendFavorites([])
        await fixture.sendFavorites(["EUR"])
        await fixture.recorder.waitUntil { $0.requestedRatePairs.contains("USD->EUR") }
        fixture.viewModel.send(.updateFromAmount("3"))

        #expect(fixture.favoriteRates.items.isEmpty)

        await gate.open()
        #expect(await waitUntil { !fixture.favoriteRates.isLoading })
        #expect(fixture.favoriteRates.items.map(\.currency.code) == ["EUR"])
        #expect(fixture.amounts == ["3.60"])
    }
}
