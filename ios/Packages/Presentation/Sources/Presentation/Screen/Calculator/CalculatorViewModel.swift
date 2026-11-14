import Domain
import Foundation
import Observation

/// 계산기 화면 ViewModel (Android `CalculatorViewModel` + Orbit MVI 대응)
///
/// View는 `send(_:)`로 의도를 전달하고, `state` 관찰과 `sideEffects()` 구독으로 결과를 받는다.
///
/// 핸들러가 저장·조회에서 기다리는 사이 MainActor가 풀려 다음 의도가 끼어든다. 겹쳐도 순서가 지켜지도록 일을 셋으로 나눈다.
/// - 확정: 계산 결과·통화 선택은 `await` 전에 `state`에 반영한다. 다음 의도는 반영된 상태를 읽는다.
/// - 저장: 로컬 저장은 `enqueuePersistence(_:)`로 탭한 순서대로 처리한다.
/// - 조회: 환율 조회는 기다리지 않고 따로 돌린다. 마지막 요청의 응답만 반영한다(`restartRateFetch()`).
///
/// Android `CalculatorViewModel`도 같은 동작을 지킨다. 두 플랫폼이 같은 시나리오로 검증한다
/// (`docs/decisions/calculator-intent-ordering.md`).
@MainActor
@Observable
public final class CalculatorViewModel {
    public private(set) var state = CalculatorState()

    private static let maxHistoryCount = 20

    @ObservationIgnored private let calculatorUseCases: CalculatorUseCases
    @ObservationIgnored private let exchangeUseCases: ExchangeUseCases
    @ObservationIgnored private let favoriteUseCases: FavoriteUseCases
    @ObservationIgnored private let currencySelectionUseCases: CurrencySelectionUseCases
    @ObservationIgnored private let reviewPromptUseCases: ReviewPromptUseCases
    @ObservationIgnored private let interstitialAdUseCases: InterstitialAdUseCases
    @ObservationIgnored private let reducer: CalculatorExpressionReducer
    @ObservationIgnored private let sideEffectBus = SideEffectBus<CalculatorSideEffect>()
    @ObservationIgnored private var observeHistoriesTask: Task<Void, Never>?
    @ObservationIgnored private var observeFavoritesTask: Task<Void, Never>?
    @ObservationIgnored private var loadCurrenciesTask: Task<Void, Never>?
    @ObservationIgnored private var persistenceTask: Task<Void, Never>?
    @ObservationIgnored private var rateFetchTask: Task<Void, Never>?
    /// 가장 최근 환율 조회의 번호. 이 번호와 다른 요청의 응답은 버린다.
    @ObservationIgnored private var latestRateRequestID = 0

    public init(
        calculatorUseCases: CalculatorUseCases,
        exchangeUseCases: ExchangeUseCases,
        favoriteUseCases: FavoriteUseCases,
        currencySelectionUseCases: CurrencySelectionUseCases,
        reviewPromptUseCases: ReviewPromptUseCases,
        interstitialAdUseCases: InterstitialAdUseCases,
    ) {
        self.calculatorUseCases = calculatorUseCases
        self.exchangeUseCases = exchangeUseCases
        self.favoriteUseCases = favoriteUseCases
        self.currencySelectionUseCases = currencySelectionUseCases
        self.reviewPromptUseCases = reviewPromptUseCases
        self.interstitialAdUseCases = interstitialAdUseCases
        reducer = CalculatorExpressionReducer(
            calculateExpression: calculatorUseCases.calculateExpression,
            convertExchangeAmount: exchangeUseCases.convertExchangeAmount,
            extractRepeatOperation: calculatorUseCases.extractRepeatOperation,
        )

        // 시작 로드는 저장 순서에 넣지 않는다. 계산이 네트워크 조회를 기다리지 않게 하려는 것이다.
        // 로드가 끝나기 전에는 통화 목록이 비어 있어 통화를 고를 수 없으므로 순서가 꼬이지 않는다.
        loadCurrenciesTask = Task { [weak self] in
            await self?.performLoadCurrencies()
        }
        observeSavedData()
    }

    deinit {
        observeHistoriesTask?.cancel()
        observeFavoritesTask?.cancel()
        loadCurrenciesTask?.cancel()
        persistenceTask?.cancel()
        rateFetchTask?.cancel()
    }

    /// 화면이 구독하는 일회성 이벤트 스트림. 구독마다 새 스트림을 반환한다.
    public func sideEffects() -> AsyncStream<CalculatorSideEffect> {
        sideEffectBus.stream()
    }

    public func send(_ intent: CalculatorIntent) {
        switch intent {
        case let .input(token): handleInput(token)
        case let .moveCursor(position): handleMoveCursor(position)
        case .delete: handleDelete()
        case .clear: handleClear()
        case .calculate: handleCalculate()
        case .clearHistory: enqueuePersistence { await $0.handleClearHistory() }
        default: sendCurrencyIntent(intent)
        }
    }

    /// 로컬 저장(기록 추가·삭제, 통화 선택, 즐겨찾기, 리뷰·광고 횟수)을 앞선 저장이 끝난 뒤에 하나씩 처리한다.
    ///
    /// 저장이 끝나는 순서가 탭 순서와 달라지면 "계산 → 기록 삭제 → 계산"이 뒤섞이거나 마지막에 고른 통화가 아닌 통화가
    /// 저장된다. 화면 상태는 `send(_:)`에서 먼저 확정하고 여기에는 저장만 넣는다. 네트워크 조회를 넣으면 "="가 기다린다.
    /// 여기서 도는 작업은 `enqueuePersistence`를 다시 부르면 안 된다. 자기 완료를 기다려 교착된다.
    private func enqueuePersistence(_ operation: @escaping (CalculatorViewModel) async -> Void) {
        let previousTask = persistenceTask
        persistenceTask = Task { [weak self] in
            await previousTask?.value
            guard let self else { return }

            await operation(self)
        }
    }

    /// 통화 선택·즐겨찾기 관련 의도 처리 (send의 분기 복잡도를 낮추기 위해 분리)
    private func sendCurrencyIntent(_ intent: CalculatorIntent) {
        switch intent {
        case let .selectMainExchangeCurrency(currency):
            handleSelectMainExchangeCurrency(currency)

        case let .selectExchangeCurrency(currency):
            handleSelectExchangeCurrency(currency)

        case let .toggleFavorite(currencyCode):
            enqueuePersistence { await $0.handleToggleFavorite(currencyCode) }

        case .swapExchangeCurrencies:
            handleSwapExchangeCurrencies()

        default: break
        }
    }

    // MARK: - 수식 입력

    private func handleInput(_ token: CalculatorToken) {
        switch token {
        case let .number(value): handleNumberInput(value)
        case let .operator(value): handleOperatorInput(value)
        case .dot: handleDotInput()
        case .parenthesis: handleParenthesisInput()
        }
    }

    private func handleMoveCursor(_ newCursorPosition: Int) {
        state.cursorPosition = min(max(newCursorPosition, 0), state.expression.count)
    }

    private func handleNumberInput(_ number: String) {
        switch reducer.inputNumber(state, number: number) {
        case .maxNumberLengthExceeded:
            sideEffectBus.send(
                .showSnackbar(message: L("숫자는 최대 \(CalculatorExpressionReducer.maxNumberLength) 자리까지 입력 가능합니다.")),
            )

        case let .updated(newState):
            state = newState
        }
    }

    private func handleOperatorInput(_ operatorSymbol: String) {
        if let newState = reducer.inputOperator(state, operatorSymbol: operatorSymbol) {
            state = newState
        }
    }

    private func handleDotInput() {
        if let newState = reducer.inputDot(state) {
            state = newState
        }
    }

    private func handleParenthesisInput() {
        state = reducer.inputParenthesis(state)
    }

    private func handleDelete() {
        if let newState = reducer.delete(state) {
            state = newState
        }
    }

    private func handleClear() {
        state.expression = ""
        state.cursorPosition = 0
        state.previewResult = ""
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""
        state.repeatOperation = nil
        state.isCalculatedResult = false
        state.isError = false
        state.errorMessage = nil
    }

    /// 현재 수식을 계산해 결과를 바로 보여 주고, 기록 저장은 그 뒤에 저장 순서대로 한다.
    ///
    /// 결과를 저장보다 먼저 확정하므로 "="를 연달아 눌러도 다음 계산이 이전 결과를 읽어 반복 연산으로 이어진다
    /// (1+2= → 3, = → 5). 환율 조회는 기다리지 않는다.
    private func handleCalculate() {
        switch reducer.calculate(state) {
        case .ignored:
            return

        case let .failed(failedState):
            state = failedState
            sideEffectBus.send(.showSnackbar(message: L("계산할 수 없는 수식입니다.")))

        case let .succeeded(calculatedState, history):
            state = calculatedState
            enqueuePersistence { await $0.saveCalculation(history) }
        }
    }

    /// 계산 기록을 저장하고 계산 성공을 센다. 저장 순서(`enqueuePersistence`) 안에서 부른다.
    ///
    /// 기록 목록에는 저장에 성공한 뒤에만 넣는다. 저장하는 동안 목록이 그대로였을 때만 저장 전 목록에 새 기록을 붙이고,
    /// 그사이 저장소의 기록 흐름이 목록을 바꿨으면 흐름을 따른다. 흐름이 새 기록을 먼저 내보냈을 때 중복되지 않고,
    /// 늦게 도착한 기존 기록을 덮어쓰지도 않는다. 기록 저장에 실패해도 계산 성공은 센다.
    private func saveCalculation(_ history: CalculatorState.HistoryItem) async {
        let historiesBeforeSave = state.histories

        do {
            try await calculatorUseCases.addHistory(
                CalculatorHistory(expression: history.expression, result: history.result),
            )
            if state.histories == historiesBeforeSave {
                state.histories = Array((historiesBeforeSave + [history]).suffix(Self.maxHistoryCount))
            }
        } catch {
            sideEffectBus.send(.showSnackbar(message: L("계산 기록을 저장하지 못했습니다.")))
        }

        // 결과를 반영한 뒤에 기록해 작업을 하나 끝낸 순간에 리뷰 창이 뜨게 한다 (Android `recordCalculationSuccess` 대응)
        if await reviewPromptUseCases.recordCalculationSuccess() {
            sideEffectBus.send(.requestReview)
        }

        // 전면 광고는 계산기를 떠날 때 MainViewModel이 정한다. 여기서는 계산 성공만 센다.
        await interstitialAdUseCases.recordCalculation()
    }

    private func handleClearHistory() async {
        do {
            try await calculatorUseCases.clearHistory()
            state.histories = []
        } catch {
            sideEffectBus.send(.showSnackbar(message: L("계산 기록을 삭제하지 못했습니다.")))
        }
    }

    private func observeSavedData() {
        let historiesStream = calculatorUseCases.getHistory()
        observeHistoriesTask = Task { [weak self] in
            for await histories in historiesStream {
                guard let self else { return }

                state.histories = histories.map {
                    CalculatorState.HistoryItem(expression: $0.expression, result: $0.result)
                }
            }
        }

        let favoritesStream = favoriteUseCases.getFavoriteCurrencies()
        observeFavoritesTask = Task { [weak self] in
            for await codes in favoritesStream {
                guard let self else { return }

                state.favoriteCurrencyCodes = codes
            }
        }
    }
}

// MARK: - 통화 선택·환율

extension CalculatorViewModel {
    private func handleToggleFavorite(_ currencyCode: String) async {
        let wasFavorite = state.favoriteCurrencyCodes.contains(currencyCode)

        do {
            try await favoriteUseCases.toggleFavoriteCurrency(currencyCode)
            sideEffectBus.send(
                .showSnackbar(message: wasFavorite ? L("즐겨찾기가 해제되었습니다.") : L("즐겨찾기에 추가되었습니다.")),
            )
        } catch {
            sideEffectBus.send(.showSnackbar(message: L("즐겨찾기 변경에 실패했습니다.")))
        }
    }

    private func performLoadCurrencies() async {
        do {
            let currencies = try await exchangeUseCases.getSupportedCurrencies()
            let deviceCurrencyCode = Locale.current.currency?.identifier ?? "KRW"
            let savedSelection = try? await currencySelectionUseCases.getCalculatorSelection()

            let mainCurrency = resolveCalculatorMainCurrency(
                currencies: currencies,
                currentCurrency: state.mainExchangeCurrency,
                savedSelection: savedSelection,
                deviceCurrencyCode: deviceCurrencyCode,
            )
            let subCurrency = resolveCalculatorSubCurrency(
                currencies: currencies,
                currentCurrency: state.selectedExchangeCurrency,
                savedSelection: savedSelection,
                mainCurrency: mainCurrency,
            )

            state.availableCurrencies = currencies
            state.mainExchangeCurrency = mainCurrency
            state.selectedExchangeCurrency = subCurrency

            restartRateFetch()
        } catch {
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
    }

    /// 지금 통화쌍의 환율 조회를 새로 시작한다. 진행 중인 조회는 취소하고 요청 번호를 올린다.
    ///
    /// 조회를 기다리지 않고 돌아오므로 계산·저장이 네트워크 응답에 묶이지 않는다. Firestore 요청은 취소돼도
    /// 끝까지 응답할 수 있지만, 요청 번호가 달라 반영하지 않는다. 통화쌍이 같아도(USD → EUR → USD) 번호로 구별한다.
    private func restartRateFetch() {
        latestRateRequestID += 1
        let requestID = latestRateRequestID
        rateFetchTask?.cancel()

        guard let from = state.mainExchangeCurrency, let to = state.selectedExchangeCurrency else {
            rateFetchTask = nil
            return
        }

        rateFetchTask = Task { [weak self] in
            await self?.performFetchExchangeRate(requestID: requestID, from: from, to: to)
        }
    }

    private func performFetchExchangeRate(requestID: Int, from: CurrencyInfo, to: CurrencyInfo) async {
        do {
            let rate: Double = if from.code == to.code {
                1.0
            } else {
                try await exchangeUseCases.getExchangeRate(from: from.code, to: to.code)
            }
            guard isLatestRateRequest(requestID, from: from, to: to) else { return }

            var newState = state
            newState.exchangeRate = rate
            state = reducer.withConvertedAmounts(newState)
        } catch {
            guard isLatestRateRequest(requestID, from: from, to: to) else { return }

            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
    }

    /// 응답이 가장 최근 요청의 것이고, 취소되지 않았고, 그사이 통화쌍도 바뀌지 않았는지 본다.
    private func isLatestRateRequest(_ requestID: Int, from: CurrencyInfo, to: CurrencyInfo) -> Bool {
        !Task.isCancelled
            && requestID == latestRateRequestID
            && state.mainExchangeCurrency?.code == from.code
            && state.selectedExchangeCurrency?.code == to.code
    }

    private func handleSwapExchangeCurrencies() {
        guard let from = state.mainExchangeCurrency, let to = state.selectedExchangeCurrency else { return }

        state.mainExchangeCurrency = to
        state.selectedExchangeCurrency = from
        state.exchangeRate = 0.0
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""
        restartRateFetch()

        enqueuePersistence {
            try? await $0.currencySelectionUseCases.saveCalculatorSelection(mainCode: to.code, subCode: from.code)
        }
    }

    private func handleSelectMainExchangeCurrency(_ currency: CurrencyInfo) {
        guard currency.code != state.mainExchangeCurrency?.code else { return }

        state.mainExchangeCurrency = currency
        state.exchangeRate = 0.0
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""
        restartRateFetch()

        enqueuePersistence {
            try? await $0.currencySelectionUseCases.saveCalculatorMainCurrency(currency.code)
        }
    }

    private func handleSelectExchangeCurrency(_ currency: CurrencyInfo) {
        guard currency.code != state.selectedExchangeCurrency?.code else { return }

        state.selectedExchangeCurrency = currency
        state.exchangeRate = 0.0
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""
        restartRateFetch()

        enqueuePersistence {
            try? await $0.currencySelectionUseCases.saveCalculatorSubCurrency(currency.code)
        }
    }
}
