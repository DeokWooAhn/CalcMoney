import Domain
import Foundation
import Observation

/// 계산기 화면 ViewModel (Android `CalculatorViewModel` + Orbit MVI 대응)
///
/// View는 `send(_:)`로 의도를 전달하고, `state` 관찰과 `sideEffects()` 구독으로 결과를 받는다.
@MainActor
@Observable
public final class CalculatorViewModel {
    public private(set) var state = CalculatorState()

    private static let maxHistoryCount = 20

    @ObservationIgnored private let calculatorUseCases: CalculatorUseCases
    @ObservationIgnored private let exchangeUseCases: ExchangeUseCases
    @ObservationIgnored private let favoriteUseCases: FavoriteUseCases
    @ObservationIgnored private let currencySelectionUseCases: CurrencySelectionUseCases
    @ObservationIgnored private let reducer: CalculatorExpressionReducer
    @ObservationIgnored private let sideEffectBus = SideEffectBus<CalculatorSideEffect>()
    @ObservationIgnored private var observeHistoriesTask: Task<Void, Never>?
    @ObservationIgnored private var observeFavoritesTask: Task<Void, Never>?
    @ObservationIgnored private var intentTask: Task<Void, Never>?

    public init(
        calculatorUseCases: CalculatorUseCases,
        exchangeUseCases: ExchangeUseCases,
        favoriteUseCases: FavoriteUseCases,
        currencySelectionUseCases: CurrencySelectionUseCases,
    ) {
        self.calculatorUseCases = calculatorUseCases
        self.exchangeUseCases = exchangeUseCases
        self.favoriteUseCases = favoriteUseCases
        self.currencySelectionUseCases = currencySelectionUseCases
        reducer = CalculatorExpressionReducer(
            calculateExpression: calculatorUseCases.calculateExpression,
            convertExchangeAmount: exchangeUseCases.convertExchangeAmount,
        )

        // Android도 Orbit container의 onCreate 블록에서 부르므로 초기 로드가 인텐트 큐에 들어간다.
        // 로드가 끝나기 전에 사용자가 통화를 바꿔도 순서가 뒤집히지 않는다.
        enqueue { await $0.performLoadCurrencies() }
        observeSavedData()
    }

    deinit {
        observeHistoriesTask?.cancel()
        observeFavoritesTask?.cancel()
        intentTask?.cancel()
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
        case .calculate: enqueue { await $0.handleCalculate() }
        case .clearHistory: enqueue { await $0.handleClearHistory() }
        default: sendCurrencyIntent(intent)
        }
    }

    /// 비동기 의도를 앞선 의도가 끝난 뒤에 하나씩 처리한다.
    ///
    /// 각 핸들러는 저장·환율 조회에서 중단되는데, 그 사이 MainActor가 풀려 다음 의도가
    /// 아직 갱신되지 않은 state를 읽는다. 그대로 두면 이런 일이 생긴다.
    /// - "=" 연타가 같은 수식을 두 번 저장하고 반복 연산(`repeatOperation`)을 건너뛴다
    /// - 통화를 빠르게 두 번 바꾸면 저장이 끝나는 순서에 따라 탭 순서와 다른 통화가 남고,
    ///   환율이 이전 통화쌍 기준으로 계산된 값으로 표시된다
    ///
    /// 연타를 버리지 않고 이어 붙이는 이유는 "=" 반복이 반복 연산이라는 정상 기능이기 때문이다
    /// (1+2= → 3, = → 5, = → 7). Android는 Orbit의 intent 파이프라인이 이 직렬화를 대신해 준다.
    ///
    /// 큐 안에서 도는 핸들러는 `enqueue`를 다시 부르면 안 된다. 자기 완료를 기다려 교착된다.
    /// (`performFetchExchangeRate`처럼 핸들러가 부르는 함수는 평범한 메서드로 남겨 둔다.)
    private func enqueue(_ operation: @escaping (CalculatorViewModel) async -> Void) {
        let previousTask = intentTask
        intentTask = Task { [weak self] in
            await previousTask?.value
            guard let self else { return }

            await operation(self)
        }
    }

    /// 통화 선택·즐겨찾기 관련 의도 처리 (send의 분기 복잡도를 낮추기 위해 분리)
    private func sendCurrencyIntent(_ intent: CalculatorIntent) {
        switch intent {
        case let .selectMainExchangeCurrency(currency):
            enqueue { await $0.handleSelectMainExchangeCurrency(currency) }

        case let .selectExchangeCurrency(currency):
            enqueue { await $0.handleSelectExchangeCurrency(currency) }

        case let .toggleFavorite(currencyCode):
            enqueue { await $0.handleToggleFavorite(currencyCode) }

        case .swapExchangeCurrencies:
            enqueue { await $0.performSwapExchangeCurrencies() }

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

    /// 현재 표현식을 계산하고(필요한 경우 반복 연산을 적용), 화면 상태를 업데이트합니다.
    ///
    /// 표현식이 비어 있으면 아무 작업도 하지 않습니다.
    /// 숫자 표현식과 반복 연산이 모두 존재하는 경우, 계산 전에 반복 연산을 표현식 뒤에 추가합니다.
    /// 계산 중 오류가 발생하면 상태를 오류로 표시하고 스낵바 사이드 이펙트를 발생시킵니다.
    /// 계산에 성공하면 표현식은 계산된 결과로 대체되고, 해당 계산 내역이 히스토리에 추가됩니다.
    private func handleCalculate() async {
        let expression = state.expression
        guard !expression.isEmpty else { return }

        let shouldRepeatOperation = state.repeatOperation != nil && Double(expression) != nil
        if !shouldRepeatOperation, !reducer.hasBinaryOperator(expression) { return }

        let expressionToCalculate = shouldRepeatOperation
            ? expression + (state.repeatOperation ?? "")
            : expression

        let result = calculatorUseCases.calculateExpression.calculate(expressionToCalculate)

        if result == "Error" {
            state.isError = true
            state.errorMessage = L("계산 오류")
            sideEffectBus.send(.showSnackbar(message: L("계산할 수 없는 수식입니다.")))
            return
        }

        let nextRepeatOperation = calculatorUseCases.extractRepeatOperation(expressionToCalculate)
            ?? state.repeatOperation

        let historyItem = CalculatorState.HistoryItem(
            expression: expressionToCalculate,
            result: result,
        )
        let nextHistories = Array((state.histories + [historyItem]).suffix(Self.maxHistoryCount))

        var isHistorySaved = true
        do {
            try await calculatorUseCases.addHistory(
                CalculatorHistory(expression: historyItem.expression, result: historyItem.result),
            )
        } catch {
            isHistorySaved = false
            sideEffectBus.send(.showSnackbar(message: L("계산 기록을 저장하지 못했습니다.")))
        }

        var newState = reducer.buildNewExpressionState(
            currentState: state,
            newExpression: result,
            newCursorPos: result.count,
        )
        newState.previewResult = ""
        newState.convertedPreviewAmount = ""
        newState.repeatOperation = nextRepeatOperation
        newState.isCalculatedResult = true
        newState.histories = isHistorySaved ? nextHistories : state.histories
        state = newState
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

            await performFetchExchangeRate()
        } catch {
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
    }

    private func performFetchExchangeRate() async {
        guard let from = state.mainExchangeCurrency, let to = state.selectedExchangeCurrency else { return }

        do {
            let rate: Double = if from.code == to.code {
                1.0
            } else {
                try await exchangeUseCases.getExchangeRate(from: from.code, to: to.code)
            }

            var newState = state
            newState.exchangeRate = rate
            state = reducer.withConvertedAmounts(newState)
        } catch {
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
    }

    private func performSwapExchangeCurrencies() async {
        guard let from = state.mainExchangeCurrency, let to = state.selectedExchangeCurrency else { return }

        state.mainExchangeCurrency = to
        state.selectedExchangeCurrency = from
        state.exchangeRate = 0.0
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""

        try? await currencySelectionUseCases.saveCalculatorSelection(mainCode: to.code, subCode: from.code)

        await performFetchExchangeRate()
    }

    private func handleSelectMainExchangeCurrency(_ currency: CurrencyInfo) async {
        guard currency.code != state.mainExchangeCurrency?.code else { return }

        try? await currencySelectionUseCases.saveCalculatorMainCurrency(currency.code)

        state.mainExchangeCurrency = currency
        state.exchangeRate = 0.0
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""

        await performFetchExchangeRate()
    }

    private func handleSelectExchangeCurrency(_ currency: CurrencyInfo) async {
        guard currency.code != state.selectedExchangeCurrency?.code else { return }

        try? await currencySelectionUseCases.saveCalculatorSubCurrency(currency.code)

        state.selectedExchangeCurrency = currency
        state.exchangeRate = 0.0
        state.convertedExpressionAmount = ""
        state.convertedPreviewAmount = ""

        await performFetchExchangeRate()
    }
}

private func resolveCalculatorMainCurrency(
    currencies: [CurrencyInfo],
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    deviceCurrencyCode: String,
) -> CurrencyInfo? {
    currencies.first { $0.code == currentCurrency?.code }
        ?? currencies.first { $0.code == savedSelection?.mainCode }
        ?? currencies.first { $0.code == deviceCurrencyCode }
        ?? currencies.first { $0.code == "KRW" }
        ?? currencies.first
}

private func resolveCalculatorSubCurrency(
    currencies: [CurrencyInfo],
    currentCurrency: CurrencyInfo?,
    savedSelection: CalculatorCurrencySelection?,
    mainCurrency: CurrencyInfo?,
) -> CurrencyInfo? {
    let mainCurrencyCode = mainCurrency?.code

    return currencies.first { $0.code == currentCurrency?.code && $0.code != mainCurrencyCode }
        ?? currencies.first { $0.code == savedSelection?.subCode && $0.code != mainCurrencyCode }
        ?? currencies.first { $0.code == "USD" && $0.code != mainCurrencyCode }
        ?? currencies.first { $0.code != mainCurrencyCode }
}
