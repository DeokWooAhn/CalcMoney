import Domain

/// 환율 화면 상태 (Android `ExchangeContract.State` 대응)
public struct ExchangeState: Equatable {
    public var fromAmount = "1"
    public var toAmount = ""
    public var fromCurrency: CurrencyInfo?
    public var toCurrency: CurrencyInfo?
    public var availableCurrencies: [CurrencyInfo] = []
    public var favoriteCurrencyCodes: [String] = []
    public var exchangeRate = 0.0
    public var exchangeRateDate = ""
    public var exchangeRateFetchedAt = 0
    public var isLoading = false

    public init() {}
}
