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
    public private(set) var state = ExchangeState()

    @ObservationIgnored private let exchangeUseCases: ExchangeUseCases
    @ObservationIgnored private let favoriteUseCases: FavoriteUseCases
    @ObservationIgnored private let currencySelectionUseCases: CurrencySelectionUseCases
    @ObservationIgnored private let sideEffectBus = SideEffectBus<ExchangeSideEffect>()
    @ObservationIgnored private var observeFavoritesTask: Task<Void, Never>?

    public init(
        exchangeUseCases: ExchangeUseCases,
        favoriteUseCases: FavoriteUseCases,
        currencySelectionUseCases: CurrencySelectionUseCases,
    ) {
        self.exchangeUseCases = exchangeUseCases
        self.favoriteUseCases = favoriteUseCases
        self.currencySelectionUseCases = currencySelectionUseCases

        Task { await performLoadCurrencies() }
        observeFavorites()
    }

    deinit {
        observeFavoritesTask?.cancel()
    }

    public func sideEffects() -> AsyncStream<ExchangeSideEffect> {
        sideEffectBus.stream()
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
        do {
            state.isLoading = true

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
            state.isLoading = false

            if selected.from != nil, selected.to != nil {
                await performFetchExchangeRate()
            }
        } catch {
            state.isLoading = false
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
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

        do {
            state.isLoading = true

            // 여기서 일시 중단(suspend)되어 응답을 기다림 (동시성 꼬임 방지)
            let rate = try await exchangeUseCases.getExchangeRate(from: requestedFrom, to: requestedTo)
            let rateDate = try await exchangeUseCases.getLatestRateDate()
            let fetchedAt = try await exchangeUseCases.getLatestFetchedAt()

            // 응답이 돌아왔을 때 상태가 이미 바뀌었으면 적용하지 않음
            guard state.fromCurrency?.code == requestedFrom, state.toCurrency?.code == requestedTo else {
                return
            }

            state.exchangeRate = rate
            state.exchangeRateDate = rateDate
            state.exchangeRateFetchedAt = fetchedAt
            state.isLoading = false
            state.toAmount = exchangeUseCases.exchangeAmount(fromAmount: state.fromAmount, rate: rate)
        } catch {
            state.isLoading = false
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
    }

    private func performRefreshExchangeRates() async {
        do {
            state.isLoading = true

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
            state.isLoading = false
            state.toAmount = exchangeUseCases.exchangeAmount(fromAmount: state.fromAmount, rate: rate)

            sideEffectBus.send(.showSnackbar(message: L("환율 정보를 새로고침했습니다.")))
        } catch {
            state.isLoading = false
            sideEffectBus.send(.showSnackbar(message: error.exchangeRateErrorMessage))
        }
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
