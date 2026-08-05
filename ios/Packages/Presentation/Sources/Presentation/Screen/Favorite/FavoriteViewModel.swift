import Domain
import Foundation
import Observation

/// 즐겨찾기 화면 ViewModel (Android `FavoriteViewModel` 대응)
///
/// 공유 ExchangeViewModel의 상태 변화를 입력받아 즐겨찾기 환율 카드 목록을 만든다.
@MainActor
@Observable
public final class FavoriteViewModel {
    public private(set) var state = FavoriteState()

    @ObservationIgnored private let exchangeUseCases: ExchangeUseCases
    @ObservationIgnored private let favoriteUseCases: FavoriteUseCases

    @ObservationIgnored private var loadTask: Task<Void, Never>?
    @ObservationIgnored private var currentBaseCurrency: CurrencyInfo?
    @ObservationIgnored private var currentBaseAmount = "1"
    @ObservationIgnored private var currentCurrencies: [CurrencyInfo] = []
    @ObservationIgnored private var currentFavoriteCodes: [String] = []
    @ObservationIgnored private var currentRateInput: FavoriteRateInput?
    @ObservationIgnored private var cachedRates: [String: Double] = [:]

    public init(exchangeUseCases: ExchangeUseCases, favoriteUseCases: FavoriteUseCases) {
        self.exchangeUseCases = exchangeUseCases
        self.favoriteUseCases = favoriteUseCases
    }

    deinit {
        loadTask?.cancel()
    }

    /// 공유 환율 상태가 바뀔 때 호출된다 (Android `onExchangeStateChanged` 대응)
    public func onExchangeStateChanged(
        fromCurrency: CurrencyInfo?,
        favoriteCurrencyCodes: [String],
        availableCurrencies: [CurrencyInfo],
        exchangeRateDate: String,
        exchangeRateFetchedAt: Int,
    ) {
        let nextRateInput = FavoriteRateInput(
            fromCurrency: fromCurrency,
            favoriteCurrencyCodes: favoriteCurrencyCodes,
            availableCurrencies: availableCurrencies,
            exchangeRateDate: exchangeRateDate,
            exchangeRateFetchedAt: exchangeRateFetchedAt,
        )

        if nextRateInput == currentRateInput, loadTask != nil || !state.items.isEmpty {
            return
        }

        currentRateInput = nextRateInput
        currentBaseCurrency = fromCurrency
        currentFavoriteCodes = favoriteCurrencyCodes
        currentCurrencies = availableCurrencies

        loadTask?.cancel()
        loadTask = nil

        guard let base = fromCurrency, !favoriteCurrencyCodes.isEmpty, !availableCurrencies.isEmpty else {
            cachedRates = [:]
            state.isLoading = false
            state.items = []
            return
        }

        loadTask = Task { [weak self] in
            guard let self else { return }

            self.state.isLoading = true

            let currencyByCode = Dictionary(
                availableCurrencies.map { ($0.code, $0) },
                uniquingKeysWith: { _, last in last },
            )
            var nextRates: [String: Double] = [:]

            for code in favoriteCurrencyCodes {
                if Task.isCancelled { return }
                if code == base.code { continue }
                if currencyByCode[code] == nil { continue }

                if let rate = try? await self.exchangeUseCases.getExchangeRate(from: base.code, to: code) {
                    nextRates[code] = rate
                }
            }

            if Task.isCancelled { return }

            self.cachedRates = nextRates
            self.rebuildItems(finishLoading: true)
            self.loadTask = nil
        }
    }

    /// 기준 금액이 바뀔 때 호출된다 (Android `onBaseAmountChanged` 대응)
    public func onBaseAmountChanged(_ fromAmount: String) {
        currentBaseAmount = fromAmount
        rebuildItems(finishLoading: false)
    }

    private func rebuildItems(finishLoading: Bool) {
        let favoriteRates = favoriteUseCases.buildFavoriteRates(
            baseCurrency: currentBaseCurrency,
            baseAmount: currentBaseAmount,
            favoriteCurrencyCodes: currentFavoriteCodes,
            availableCurrencies: currentCurrencies,
            ratesByCode: cachedRates,
        )

        state.items = favoriteRates.map { rateInfo in
            FavoriteState.Item(
                currency: rateInfo.currency,
                convertedAmount: String(format: "%.2f", rateInfo.convertedAmount),
                rateLabel: "1 \(rateInfo.baseCurrencyCode) = "
                    + String(format: "%.4f", rateInfo.rate)
                    + " \(rateInfo.currency.code)",
            )
        }

        if finishLoading {
            state.isLoading = false
        }
    }
}

private struct FavoriteRateInput: Equatable {
    let fromCurrency: CurrencyInfo?
    let favoriteCurrencyCodes: [String]
    let availableCurrencies: [CurrencyInfo]
    let exchangeRateDate: String
    let exchangeRateFetchedAt: Int
}
