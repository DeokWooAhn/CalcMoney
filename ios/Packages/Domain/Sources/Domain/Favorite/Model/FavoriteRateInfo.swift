/// 즐겨찾기 통화의 환율·환산 결과 (Android `FavoriteRateInfo` 대응)
public struct FavoriteRateInfo: Equatable, Sendable {
    public let currency: CurrencyInfo
    public let baseCurrencyCode: String
    public let rate: Double
    public let convertedAmount: Double

    public init(currency: CurrencyInfo, baseCurrencyCode: String, rate: Double, convertedAmount: Double) {
        self.currency = currency
        self.baseCurrencyCode = baseCurrencyCode
        self.rate = rate
        self.convertedAmount = convertedAmount
    }
}
