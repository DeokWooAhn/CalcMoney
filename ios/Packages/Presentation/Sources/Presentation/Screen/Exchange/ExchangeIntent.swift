import Domain

/// 환율 화면 의도 (Android `ExchangeContract.Intent` 대응)
public enum ExchangeIntent: Equatable {
    case updateFromAmount(String)
    case selectFromCurrency(CurrencyInfo)
    case selectToCurrency(CurrencyInfo)
    case toggleFavorite(String)
    case swapCurrencies
    case loadCurrencies
    case refreshExchangeRates
}
