import Domain
import Foundation
@testable import Presentation

/// 저장·조회 호출을 순서대로 기록한다. 인텐트 직렬화 검증에 쓴다.
actor CalculatorTestRecorder {
    private(set) var savedHistories: [CalculatorHistory] = []
    private(set) var savedMainCurrencyCodes: [String] = []
    private(set) var requestedRatePairs: [String] = []

    func recordHistory(_ history: CalculatorHistory) {
        savedHistories.append(history)
    }

    func recordMainCurrency(_ code: String) {
        savedMainCurrencyCodes.append(code)
    }

    func recordRatePair(from: String, to: String) {
        requestedRatePairs.append("\(from)->\(to)")
    }

    private(set) var refreshCount = 0

    func recordRefresh() {
        refreshCount += 1
    }
}

/// 저장이 즉시 끝나지 않는 상황(디스크·네트워크)을 흉내 낸다.
/// 이 중단 지점에서 MainActor가 풀리며 다음 의도가 끼어들 수 있다.
struct FakeCalculatorHistoryRepository: CalculatorHistoryRepository {
    let recorder: CalculatorTestRecorder
    var saveDelay: Duration = .milliseconds(20)

    func histories() -> AsyncStream<[CalculatorHistory]> {
        AsyncStream { $0.finish() }
    }

    func addHistory(_ history: CalculatorHistory) async throws {
        await Task.yield()
        try? await Task.sleep(for: saveDelay)

        await recorder.recordHistory(history)
    }

    func clearHistories() async throws {}
}

struct FakeExchangeRateRepository: ExchangeRateRepository {
    let recorder: CalculatorTestRecorder
    var currencies: [CurrencyInfo] = []
    var rateDate = ""
    /// 새로고침이 즉시 끝나지 않는 상황을 흉내 낸다. 호출자가 완료를 기다리는지 검증할 때 쓴다.
    var refreshDelay: Duration = .zero

    func exchangeRate(from: String, to: String) async throws -> Double {
        await recorder.recordRatePair(from: from, to: to)
        await Task.yield()

        return 1.0
    }

    func latestRateDate() async throws -> String {
        rateDate
    }

    func latestFetchedAt() async throws -> Int {
        0
    }

    func refreshExchangeRates() async throws {
        await Task.yield()

        if refreshDelay > .zero {
            try? await Task.sleep(for: refreshDelay)
        }

        await recorder.recordRefresh()
    }

    func supportedCurrencies() async throws -> [CurrencyInfo] {
        currencies
    }
}

struct FakeFavoriteCurrencyRepository: FavoriteCurrencyRepository {
    func favoriteCurrencyCodes() -> AsyncStream<[String]> {
        AsyncStream { $0.finish() }
    }

    func addFavorite(_: String) async throws {}

    func removeFavorite(_: String) async throws {}

    func isFavorite(_: String) async throws -> Bool {
        false
    }
}

struct FakeCurrencySelectionRepository: CurrencySelectionRepository {
    let recorder: CalculatorTestRecorder
    var savedSelection = CalculatorCurrencySelection(mainCode: nil, subCode: nil)
    /// 통화 코드별 저장 지연. 저장이 끝나는 순서를 탭 순서와 어긋나게 만들어 레이스를 재현한다.
    var saveDelays: [String: Duration] = [:]

    func calculatorSelection() async throws -> CalculatorCurrencySelection {
        savedSelection
    }

    func saveCalculatorMainCurrencyCode(_ code: String) async throws {
        await Task.yield()
        if let delay = saveDelays[code] {
            try? await Task.sleep(for: delay)
        }

        await recorder.recordMainCurrency(code)
    }

    func saveCalculatorSubCurrencyCode(_: String) async throws {}

    func saveCalculatorSelection(mainCode _: String, subCode _: String) async throws {}

    func exchangeSelection() async throws -> ExchangeCurrencySelection {
        ExchangeCurrencySelection(fromCode: nil, toCode: nil)
    }

    func saveExchangeFromCurrencyCode(_: String) async throws {}

    func saveExchangeToCurrencyCode(_: String) async throws {}

    func saveExchangeSelection(fromCode _: String, toCode _: String) async throws {}
}

@MainActor
enum CalculatorTestEnvironment {
    static func makeViewModel(
        history: FakeCalculatorHistoryRepository,
        exchangeRate: FakeExchangeRateRepository,
        currencySelection: FakeCurrencySelectionRepository,
    ) -> CalculatorViewModel {
        let favorite = FakeFavoriteCurrencyRepository()
        let calculateExpression = CalculateExpressionUseCase()

        return CalculatorViewModel(
            calculatorUseCases: CalculatorUseCases(
                addHistory: AddCalculatorHistoryUseCase(repository: history),
                calculateExpression: calculateExpression,
                clearHistory: ClearCalculatorHistoriesUseCase(repository: history),
                extractRepeatOperation: ExtractRepeatOperationUseCase(),
                getHistory: GetCalculatorHistoriesUseCase(repository: history),
            ),
            exchangeUseCases: ExchangeUseCases(
                exchangeAmount: CalculateExchangeAmountUseCase(),
                convertExchangeAmount: ConvertExchangeAmountUseCase(calculateExpression: calculateExpression),
                getExchangeRate: GetExchangeRateUseCase(repository: exchangeRate),
                getLatestRateDate: GetLatestExchangeRateDateUseCase(repository: exchangeRate),
                getLatestFetchedAt: GetLatestExchangeRateFetchedAtUseCase(repository: exchangeRate),
                refreshExchangeRates: RefreshExchangeRatesUseCase(repository: exchangeRate),
                getSupportedCurrencies: GetSupportedCurrenciesUseCase(repository: exchangeRate),
            ),
            favoriteUseCases: FavoriteUseCases(
                buildFavoriteRates: BuildFavoriteRatesUseCase(),
                getFavoriteCurrencies: GetFavoriteCurrenciesUseCase(repository: favorite),
                toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase(repository: favorite),
            ),
            currencySelectionUseCases: CurrencySelectionUseCases(
                getCalculatorSelection: GetCalculatorSelectionUseCase(repository: currencySelection),
                saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase(repository: currencySelection),
                saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase(repository: currencySelection),
                saveCalculatorSelection: SaveCalculatorSelectionUseCase(repository: currencySelection),
                getExchangeSelection: GetExchangeSelectionUseCase(repository: currencySelection),
                saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase(repository: currencySelection),
                saveExchangeToCurrency: SaveExchangeToCurrencyUseCase(repository: currencySelection),
                saveExchangeSelection: SaveExchangeSelectionUseCase(repository: currencySelection),
            ),
        )
    }
}
