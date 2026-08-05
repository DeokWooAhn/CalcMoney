import Domain

/// 즐겨찾기 화면 상태 (Android `FavoriteContract` 대응)
public struct FavoriteState: Equatable {
    public struct Item: Equatable {
        public let currency: CurrencyInfo
        public let convertedAmount: String
        public let rateLabel: String

        public init(currency: CurrencyInfo, convertedAmount: String, rateLabel: String) {
            self.currency = currency
            self.convertedAmount = convertedAmount
            self.rateLabel = rateLabel
        }
    }

    public var isLoading = false
    public var items: [Item] = []

    public init() {}
}
