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
    /// 즐겨찾기가 현재 기준 통화 하나뿐일 때의 기준 통화 코드.
    /// 기준 통화는 카드로 만들지 않으므로 목록이 비는데, 이를 로드 실패와 구분하는 데 쓴다.
    public var baseOnlyFavoriteCode: String?

    public init() {}
}
