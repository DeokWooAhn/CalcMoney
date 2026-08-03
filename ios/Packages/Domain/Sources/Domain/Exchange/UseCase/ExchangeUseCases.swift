/// 환율 화면에서 쓰는 유스케이스 묶음 (Android `ExchangeUseCases` 대응)
public struct ExchangeUseCases: Sendable {
    public let exchangeAmount: CalculateExchangeAmountUseCase
    public let convertExchangeAmount: ConvertExchangeAmountUseCase
    public let getExchangeRate: GetExchangeRateUseCase
    public let getLatestRateDate: GetLatestExchangeRateDateUseCase
    public let getLatestFetchedAt: GetLatestExchangeRateFetchedAtUseCase
    public let refreshExchangeRates: RefreshExchangeRatesUseCase
    public let getSupportedCurrencies: GetSupportedCurrenciesUseCase

    public init(
        exchangeAmount: CalculateExchangeAmountUseCase,
        convertExchangeAmount: ConvertExchangeAmountUseCase,
        getExchangeRate: GetExchangeRateUseCase,
        getLatestRateDate: GetLatestExchangeRateDateUseCase,
        getLatestFetchedAt: GetLatestExchangeRateFetchedAtUseCase,
        refreshExchangeRates: RefreshExchangeRatesUseCase,
        getSupportedCurrencies: GetSupportedCurrenciesUseCase,
    ) {
        self.exchangeAmount = exchangeAmount
        self.convertExchangeAmount = convertExchangeAmount
        self.getExchangeRate = getExchangeRate
        self.getLatestRateDate = getLatestRateDate
        self.getLatestFetchedAt = getLatestFetchedAt
        self.refreshExchangeRates = refreshExchangeRates
        self.getSupportedCurrencies = getSupportedCurrencies
    }
}
