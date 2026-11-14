import Domain
import Foundation
@testable import Presentation

/// 저장·조회 호출을 순서대로 기록한다. 인텐트 직렬화 검증에 쓴다.
actor CalculatorTestRecorder {
    private(set) var savedHistories: [CalculatorHistory] = []
    /// 기록 저장이 끝난 순서. 추가는 `add:<수식>`, 삭제는 `clear`로 남긴다.
    private(set) var savedOperations: [String] = []
    private(set) var savedMainCurrencyCodes: [String] = []
    private(set) var requestedRatePairs: [String] = []
    /// 응답(성공·실패)까지 끝난 환율 조회. 요청 순서가 아니라 끝난 순서다.
    private(set) var completedRatePairs: [String] = []
    private(set) var refreshCount = 0

    /// 기록이 늘 때마다 신호를 보낸다. `waitUntil(_:)`이 이 신호를 받을 때마다 조건을 다시 확인한다.
    private let changes: AsyncStream<Void>
    private let changesContinuation: AsyncStream<Void>.Continuation

    init() {
        (changes, changesContinuation) = AsyncStream.makeStream(bufferingPolicy: .bufferingNewest(1))
    }

    func recordHistory(_ history: CalculatorHistory) {
        savedHistories.append(history)
        savedOperations.append("add:\(history.expression)")
        changesContinuation.yield()
    }

    func recordClear() {
        savedOperations.append("clear")
        changesContinuation.yield()
    }

    func recordRateCompletion(_ pair: String) {
        completedRatePairs.append(pair)
        changesContinuation.yield()
    }

    func recordMainCurrency(_ code: String) {
        savedMainCurrencyCodes.append(code)
        changesContinuation.yield()
    }

    func recordRatePair(from: String, to: String) {
        requestedRatePairs.append("\(from)->\(to)")
        changesContinuation.yield()
    }

    func recordRefresh() {
        refreshCount += 1
        changesContinuation.yield()
    }

    /// 기록이 조건을 만족할 때까지 기다린다. 고정 시간 대기 대신 저장·조회가 끝났는지 확인할 때 쓴다.
    ///
    /// 기록은 늘기만 하므로 "몇 건 이상"처럼 한 번 참이 되면 되돌아가지 않는 조건을 넘길 것.
    /// 조건이 끝내 참이 되지 않으면 스위트의 `.timeLimit`이 테스트를 취소하고, 그때 대기를 푼다.
    /// 신호 스트림은 하나이므로 한 번에 한 곳에서만 기다린다.
    func waitUntil(_ condition: @Sendable (isolated CalculatorTestRecorder) -> Bool) async {
        guard !condition(self) else { return }

        for await _ in changes where condition(self) {
            return
        }
    }
}

/// 저장이 즉시 끝나지 않는 상황(디스크·네트워크)을 흉내 낸다.
/// 이 중단 지점에서 MainActor가 풀리며 다음 의도가 끼어들 수 있다.
struct FakeCalculatorHistoryRepository: CalculatorHistoryRepository {
    let recorder: CalculatorTestRecorder
    var saveDelay: Duration = .milliseconds(20)
    /// 있으면 저장할 때 이 오류를 던진다.
    var saveError: (any Error)?
    /// 있으면 기록 흐름을 이 피드로 내보낸다. 없으면 흐름은 바로 끝난다.
    var historyFeed: HistoryFeed?

    func histories() -> AsyncStream<[CalculatorHistory]> {
        historyFeed?.stream ?? AsyncStream { $0.finish() }
    }

    func addHistory(_ history: CalculatorHistory) async throws {
        await Task.yield()
        try? await Task.sleep(for: saveDelay)

        if let saveError {
            throw saveError
        }
        await recorder.recordHistory(history)
        await historyFeed?.didSave(history)
    }

    func clearHistories() async throws {
        await Task.yield()

        await recorder.recordClear()
    }
}

/// 저장소의 기록 흐름을 흉내 낸다. 한 번만 저장하는 시나리오에서 쓴다.
///
/// `storedBeforeLaunch`는 앱을 켜기 전부터 있던 기록이다. 시작 로드가 늦은 상황을 만들려고 처음에는 내보내지 않고,
/// 저장이 끝날 때 방금 저장한 기록과 함께 내보낸다.
final class HistoryFeed: Sendable {
    let stream: AsyncStream<[CalculatorHistory]>
    private let continuation: AsyncStream<[CalculatorHistory]>.Continuation
    private let storedBeforeLaunch: [CalculatorHistory]

    init(storedBeforeLaunch: [CalculatorHistory]) {
        (stream, continuation) = AsyncStream.makeStream()
        self.storedBeforeLaunch = storedBeforeLaunch
    }

    /// 저장 직후 흐름을 내보내고, ViewModel이 그 목록을 받을 틈을 준 뒤 돌아온다.
    func didSave(_ history: CalculatorHistory) async {
        continuation.yield(storedBeforeLaunch + [history])
        try? await Task.sleep(for: .milliseconds(20))
    }
}

/// 환율 조회 한 번의 응답. `gate`가 있으면 열릴 때까지, `delay`가 있으면 그만큼 기다린 뒤 응답한다.
struct RateStep {
    var delay: Duration = .zero
    var gate: TestGate?
    var rate = 1.0
    var error: (any Error)?
}

/// 통화쌍별로 몇 번째 조회에 어떻게 응답할지 정한다. 적어 두지 않은 조회는 바로 1.0을 돌려준다.
actor RateScript {
    private var steps: [String: [RateStep]]

    init(_ steps: [String: [RateStep]] = [:]) {
        self.steps = steps
    }

    func next(for pair: String) -> RateStep {
        guard var queue = steps[pair], !queue.isEmpty else { return RateStep() }

        let step = queue.removeFirst()
        steps[pair] = queue
        return step
    }
}

/// 테스트가 열 때까지 기다리게 하는 문. 끝나지 않은 네트워크 요청을 흉내 낸다.
actor TestGate {
    private var isOpen = false
    private var waiters: [CheckedContinuation<Void, Never>] = []

    func wait() async {
        guard !isOpen else { return }

        await withCheckedContinuation { waiters.append($0) }
    }

    func open() {
        isOpen = true
        waiters.forEach { $0.resume() }
        waiters.removeAll()
    }
}

struct FakeExchangeRateRepository: ExchangeRateRepository {
    let recorder: CalculatorTestRecorder
    var currencies: [CurrencyInfo] = []
    var rateDate = ""
    /// 새로고침이 즉시 끝나지 않는 상황을 흉내 낸다. 호출자가 완료를 기다리는지 검증할 때 쓴다.
    var refreshDelay: Duration = .zero
    /// 통화 목록 조회를 이 문이 열릴 때까지 붙잡아 둔다. 시작 로드가 끝나지 않은 상황을 만든다.
    var currenciesGate: TestGate?
    var rateScript = RateScript()

    func exchangeRate(from: String, to: String) async throws -> Double {
        let pair = "\(from)->\(to)"
        await recorder.recordRatePair(from: from, to: to)
        let step = await rateScript.next(for: pair)
        await Task.yield()

        if let gate = step.gate {
            await gate.wait()
        }
        if step.delay > .zero {
            // 실제 Firestore 요청처럼 호출한 쪽이 취소해도 멈추지 않게 분리된 Task에서 기다린다.
            let delay = step.delay
            await Task.detached { try? await Task.sleep(for: delay) }.value
        }

        await recorder.recordRateCompletion(pair)
        if let error = step.error {
            throw error
        }
        return step.rate
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
        if let currenciesGate {
            await currenciesGate.wait()
        }

        return currencies
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

/// 인앱 리뷰 요청 기록을 메모리에 들고 있는다. 기본값은 빈 기록이라 리뷰 요청 조건을 만족하지 않는다.
actor FakeReviewPromptRepository: ReviewPromptRepository {
    private(set) var record: ReviewPromptRecord

    /// 기록이 바뀔 때마다 신호를 보낸다. `waitUntil(_:)`이 이 신호를 받을 때마다 조건을 다시 확인한다.
    private let changes: AsyncStream<Void>
    private let changesContinuation: AsyncStream<Void>.Continuation

    init(record: ReviewPromptRecord = ReviewPromptRecord()) {
        self.record = record
        (changes, changesContinuation) = AsyncStream.makeStream(bufferingPolicy: .bufferingNewest(1))
    }

    func updateRecord<Result: Sendable>(
        _ transform: @Sendable (inout ReviewPromptRecord) -> Result,
    ) async -> Result {
        let result = transform(&record)
        changesContinuation.yield()

        return result
    }

    /// 기록이 조건을 만족할 때까지 기다린다. `CalculatorTestRecorder.waitUntil(_:)`과 같은 방식이다.
    func waitUntil(_ condition: @Sendable (ReviewPromptRecord) -> Bool) async {
        guard !condition(record) else { return }

        for await _ in changes where condition(record) {
            return
        }
    }
}

/// 메모리에 전면 광고 기록을 들고 있는 테스트용 저장소
actor FakeInterstitialAdRepository: InterstitialAdRepository {
    private(set) var current: InterstitialAdRecord

    /// 기록이 바뀔 때마다 신호를 보낸다. `waitUntil(_:)`이 이 신호를 받을 때마다 조건을 다시 확인한다.
    private let changes: AsyncStream<Void>
    private let changesContinuation: AsyncStream<Void>.Continuation

    init(record: InterstitialAdRecord = InterstitialAdRecord()) {
        current = record
        (changes, changesContinuation) = AsyncStream.makeStream(bufferingPolicy: .bufferingNewest(1))
    }

    func record() async -> InterstitialAdRecord {
        current
    }

    func updateRecord(_ transform: @Sendable (inout InterstitialAdRecord) -> Void) async {
        transform(&current)
        changesContinuation.yield()
    }

    /// 기록이 조건을 만족할 때까지 기다린다. `FakeReviewPromptRepository.waitUntil(_:)`과 같은 방식이다.
    func waitUntil(_ condition: @Sendable (InterstitialAdRecord) -> Bool) async {
        guard !condition(current) else { return }

        for await _ in changes where condition(current) {
            return
        }
    }
}

@MainActor
enum CalculatorTestEnvironment {
    static func makeViewModel(
        history: FakeCalculatorHistoryRepository,
        exchangeRate: FakeExchangeRateRepository,
        currencySelection: FakeCurrencySelectionRepository,
        reviewPrompt: FakeReviewPromptRepository = FakeReviewPromptRepository(),
        interstitialAd: FakeInterstitialAdRepository = FakeInterstitialAdRepository(),
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
            reviewPromptUseCases: ReviewPromptUseCases(
                recordAppOpen: RecordAppOpenUseCase(repository: reviewPrompt),
                recordCalculationSuccess: RecordCalculationSuccessUseCase(repository: reviewPrompt),
            ),
            interstitialAdUseCases: InterstitialAdUseCases(
                recordCalculation: RecordInterstitialAdCalculationUseCase(repository: interstitialAd),
                shouldShow: ShouldShowInterstitialAdUseCase(repository: interstitialAd),
                recordShown: RecordInterstitialAdShownUseCase(repository: interstitialAd),
            ),
        )
    }
}
