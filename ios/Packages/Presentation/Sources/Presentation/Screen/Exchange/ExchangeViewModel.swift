import Domain
import Foundation
import Observation

/// 환율 화면 ViewModel (Android `ExchangeViewModel` 대응)
///
/// Android의 activity-scoped 공유와 같게, 앱에서 하나만 만들어
/// Exchange·Favorite·Setting 화면이 함께 쓴다.
@MainActor
@Observable
public final class ExchangeViewModel {
    /// 바뀔 때마다 즐겨찾기 카드를 다시 모을지 한 곳에서 판단한다(`ExchangeViewModel+FavoriteRates.swift`).
    public private(set) var state = ExchangeState() {
        didSet { stateDidChangeForFavoriteRates() }
    }

    @ObservationIgnored private let exchangeUseCases: ExchangeUseCases
    @ObservationIgnored private let favoriteUseCases: FavoriteUseCases
    @ObservationIgnored private let currencySelectionUseCases: CurrencySelectionUseCases
    @ObservationIgnored private let sideEffectBus = SideEffectBus<ExchangeSideEffect>()
    @ObservationIgnored private var observeFavoritesTask: Task<Void, Never>?
    @ObservationIgnored private var loadCurrenciesTask: Task<Void, Never>?
    @ObservationIgnored private var inFlightLoadCount = 0

    // 즐겨찾기 카드 상태. 확장 파일(`ExchangeViewModel+FavoriteRates.swift`)이 쓰므로 `private`으로 두지 않는다.
    @ObservationIgnored let favoriteRatesLoader: FavoriteRatesLoader
    @ObservationIgnored var favoriteRatesTask: Task<Void, Never>?
    /// 마지막으로 본 즐겨찾기 입력. 이 값이 바뀔 때만 환율을 다시 모은다.
    @ObservationIgnored var observedFavoriteRateInput: FavoriteRateInput?
    /// 마지막으로 본 기준 금액. 금액만 바뀌면 다시 조회하지 않고 카드만 다시 만든다.
    @ObservationIgnored var observedFavoriteBaseAmount: String?
    /// 마지막으로 조회를 마친 입력과 환율. 없으면 아직 조회를 마친 적이 없거나 카드를 비운 상태다.
    @ObservationIgnored var loadedFavoriteRates: LoadedFavoriteRates?

    public init(
        exchangeUseCases: ExchangeUseCases,
        favoriteUseCases: FavoriteUseCases,
        currencySelectionUseCases: CurrencySelectionUseCases,
    ) {
        self.exchangeUseCases = exchangeUseCases
        self.favoriteUseCases = favoriteUseCases
        self.currencySelectionUseCases = currencySelectionUseCases
        favoriteRatesLoader = FavoriteRatesLoader(
            exchangeUseCases: exchangeUseCases,
            favoriteUseCases: favoriteUseCases,
        )

        loadCurrenciesTask = Task { await performLoadCurrencies() }
        observeFavorites()
    }

    deinit {
        observeFavoritesTask?.cancel()
        loadCurrenciesTask?.cancel()
        favoriteRatesTask?.cancel()
    }

    public func sideEffects() -> AsyncStream<ExchangeSideEffect> {
        sideEffectBus.stream()
    }

    /// 완료를 기다려야 하는 호출자를 위한 진입점 (당겨서 새로고침 등).
    ///
    /// `send(.refreshExchangeRates)`는 내부에서 Task를 띄우고 즉시 반환하므로
    /// `.refreshable`에 그대로 쓰면 새로고침 표시가 곧바로 사라진다.
    public func refreshExchangeRates() async {
        await performRefreshExchangeRates()
    }

    public func send(_ intent: ExchangeIntent) {
        switch intent {
        case let .updateFromAmount(amount): handleUpdateFromAmount(amount)
        case let .selectFromCurrency(currency): Task { await handleSelectFromCurrency(currency) }
        case let .selectToCurrency(currency): Task { await handleSelectToCurrency(currency) }
        case let .toggleFavorite(currencyCode): Task { await handleToggleFavorite(currencyCode) }
        case .swapCurrencies: Task { await performSwapCurrencies() }
        case .loadCurrencies: Task { await performLoadCurrencies() }
        case .refreshExchangeRates: Task { await performRefreshExchangeRates() }
        }
    }

    private func handleUpdateFromAmount(_ amount: String) {
        guard isValidAmountInput(amount) else { return }

        state.fromAmount = amount
        state.toAmount = exchangeUseCases.exchangeAmount(fromAmount: amount, rate: state.exchangeRate)
    }

    /// 숫자와 소수점 하나만 허용한다 (Android `^\d*\.?\d*$` 대응)
    private func isValidAmountInput(_ amount: String) -> Bool {
        amount.allSatisfy { $0.isNumber || $0 == "." } && amount.count(where: { $0 == "." }) <= 1
    }

    private func handleSelectFromCurrency(_ currency: CurrencyInfo) async {
        if currency == state.toCurrency {
            await performSwapCurrencies()
        } else {
            try? await currencySelectionUseCases.saveExchangeFromCurrency(currency.code)

            state.fromCurrency = currency
            await performFetchExchangeRate()
        }
    }

    private func handleSelectToCurrency(_ currency: CurrencyInfo) async {
        if currency == state.fromCurrency {
            await performSwapCurrencies()
        } else {
            try? await currencySelectionUseCases.saveExchangeToCurrency(currency.code)

            state.toCurrency = currency
            await performFetchExchangeRate()
        }
    }

    private func observeFavorites() {
        let favoritesStream = favoriteUseCases.getFavoriteCurrencies()

        observeFavoritesTask = Task { [weak self] in
            for await codes in favoritesStream {
                guard let self else { return }

                state.favoriteCurrencyCodes = codes
            }
        }
    }

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
        beginLoading()
        defer { endLoading() }

        do {
            let currencies = try await exchangeUseCases.getSupportedCurrencies()
            let savedSelection = try? await currencySelectionUseCases.getExchangeSelection()
            let selected = resolveExchangeCurrencies(
                currencies: currencies,
                savedSelection: savedSelection,
                currentFromCurrency: state.fromCurrency,
                currentToCurrency: state.toCurrency,
            )

            state.availableCurrencies = currencies
            state.fromCurrency = selected.from
            state.toCurrency = selected.to
            state.failure = nil

            if selected.from != nil, selected.to != nil {
                await performFetchExchangeRate()
            }
        } catch {
            state.failure = ExchangeFailure(scope: .currencies, message: error.exchangeRateErrorMessage)
        }
    }

    private func resolveExchangeCurrencies(
        currencies: [CurrencyInfo],
        savedSelection: ExchangeCurrencySelection?,
        currentFromCurrency: CurrencyInfo?,
        currentToCurrency: CurrencyInfo?,
    ) -> (from: CurrencyInfo?, to: CurrencyInfo?) {
        let krw = currencies.first { $0.code == "KRW" }
        let usd = currencies.first { $0.code == "USD" }

        let fromCurrency = currencies.first { $0.code == currentFromCurrency?.code }
            ?? currencies.first { $0.code == savedSelection?.fromCode }
            ?? usd
            ?? currencies.first
            ?? currentFromCurrency

        let fromCode = fromCurrency?.code
        let toCurrency = currencies.first { $0.code == currentToCurrency?.code }
            ?? currencies.first { $0.code == savedSelection?.toCode && $0.code != fromCode }
            ?? krw.flatMap { $0.code != fromCode ? $0 : nil }
            ?? currencies.first { $0.code != fromCode }
            ?? currentToCurrency

        return (fromCurrency, toCurrency)
    }

    private func performSwapCurrencies() async {
        guard let newFrom = state.toCurrency, let newTo = state.fromCurrency else { return }

        state.fromCurrency = newFrom
        state.toCurrency = newTo
        state.fromAmount = state.toAmount.isEmpty ? "1" : state.toAmount

        try? await currencySelectionUseCases.saveExchangeSelection(
            fromCode: newFrom.code,
            toCode: newTo.code,
        )

        await performFetchExchangeRate()
    }

    private func performFetchExchangeRate() async {
        guard let fromCurrency = state.fromCurrency, let toCurrency = state.toCurrency else { return }

        let requestedFrom = fromCurrency.code
        let requestedTo = toCurrency.code

        beginLoading()
        defer { endLoading() }

        do {
            // 여기서 일시 중단(suspend)되어 응답을 기다림 (동시성 꼬임 방지)
            let rate = try await exchangeUseCases.getExchangeRate(from: requestedFrom, to: requestedTo)
            let rateDate = try await exchangeUseCases.getLatestRateDate()
            let fetchedAt = try await exchangeUseCases.getLatestFetchedAt()

            // 응답이 돌아왔을 때 상태가 이미 바뀌었으면 적용하지 않음
            guard isCurrentRequest(from: requestedFrom, to: requestedTo) else {
                return
            }

            state.exchangeRate = rate
            state.exchangeRateDate = rateDate
            state.exchangeRateFetchedAt = fetchedAt
            state.toAmount = exchangeUseCases.exchangeAmount(fromAmount: state.fromAmount, rate: rate)
            state.failure = nil
        } catch {
            guard isCurrentRequest(from: requestedFrom, to: requestedTo) else {
                return
            }
            state.failure = ExchangeFailure(scope: .rate, message: error.exchangeRateErrorMessage)
        }
    }

    private func isCurrentRequest(from: String, to: String) -> Bool {
        state.fromCurrency?.code == from && state.toCurrency?.code == to
    }

    /// 로딩 표시는 진행 중인 요청이 모두 끝났을 때만 끈다.
    ///
    /// 해제를 각 경로에 흩어 두면 빠뜨려 스피너가 계속 도는 상태로 남기 쉬우므로 `defer`로 모으는데,
    /// 그대로 두면 지난 요청이 끝나면서 그 사이 시작된 요청의 스피너까지 꺼 버린다.
    /// 초기 로드가 환율 조회를 품고 있어 중첩 호출도 같은 문제를 일으킨다.
    private func beginLoading() {
        inFlightLoadCount += 1
        state.isLoading = true
    }

    private func endLoading() {
        inFlightLoadCount = max(0, inFlightLoadCount - 1)
        if inFlightLoadCount == 0 {
            state.isLoading = false
        }
    }

    private func performRefreshExchangeRates() async {
        beginLoading()
        defer { endLoading() }

        do {
            try await exchangeUseCases.refreshExchangeRates()

            let currencies = try await exchangeUseCases.getSupportedCurrencies()
            let savedSelection = try? await currencySelectionUseCases.getExchangeSelection()
            let selected = resolveExchangeCurrencies(
                currencies: currencies,
                savedSelection: savedSelection,
                currentFromCurrency: state.fromCurrency,
                currentToCurrency: state.toCurrency,
            )
            let rate = try await exchangeRateOrDefault(fromCurrency: selected.from, toCurrency: selected.to)
            let rateDate = try await exchangeUseCases.getLatestRateDate()
            let fetchedAt = try await exchangeUseCases.getLatestFetchedAt()

            state.availableCurrencies = currencies
            state.fromCurrency = selected.from
            state.toCurrency = selected.to
            state.exchangeRate = rate
            state.exchangeRateDate = rateDate
            state.exchangeRateFetchedAt = fetchedAt
            state.toAmount = exchangeUseCases.exchangeAmount(fromAmount: state.fromAmount, rate: rate)
            state.failure = nil
            state.ratesRefreshCount += 1

            sideEffectBus.send(.showSnackbar(message: L("환율 정보를 새로고침했습니다.")))
        } catch {
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
    }

    /// 즐겨찾기 카드 상태가 실제로 바뀔 때만 대입한다. 같은 값을 다시 넣어 관찰자를 깨우지 않는다.
    ///
    /// `state`의 setter는 이 파일 밖에 열려 있지 않아 확장 파일은 이 함수로만 카드를 바꾼다.
    func reduceFavoriteRates(_ transform: (FavoriteRates) -> FavoriteRates) {
        let next = transform(state.favoriteRates)
        guard next != state.favoriteRates else { return }

        state.favoriteRates = next
    }

    private func exchangeRateOrDefault(
        fromCurrency: CurrencyInfo?,
        toCurrency: CurrencyInfo?,
    ) async throws -> Double {
        guard let fromCurrency, let toCurrency else { return 0.0 }
        if fromCurrency.code == toCurrency.code { return 1.0 }

        return try await exchangeUseCases.getExchangeRate(from: fromCurrency.code, to: toCurrency.code)
    }
}
