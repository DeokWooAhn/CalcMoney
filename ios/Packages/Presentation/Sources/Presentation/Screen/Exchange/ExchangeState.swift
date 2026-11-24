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
    public var failure: ExchangeFailure?
    /// 즐겨찾기 화면의 카드. 기준 통화·금액이 환율 화면과 같아 이 ViewModel이 만든다.
    public var favoriteRates = FavoriteRates()
    /// 수동 새로고침이 성공한 횟수. 같은 서버 문서를 다시 받아도 즐겨찾기 환율을 다시 모으게 하는 신호다.
    public var ratesRefreshCount = 0

    public init() {}
}

/// 즐겨찾기 화면 상태 (Android `ExchangeContract.FavoriteRates` 대응)
public struct FavoriteRates: Equatable {
    public var isLoading = false
    public var items: [FavoriteRateItem] = []
    /// 즐겨찾기가 현재 기준 통화 하나뿐일 때의 기준 통화 코드.
    /// 기준 통화는 카드로 만들지 않으므로 목록이 비는데, 이를 로드 실패와 구분하는 데 쓴다.
    public var baseOnlyFavoriteCode: String?

    public init() {}
}

/// 즐겨찾기 카드 하나 (Android `ExchangeContract.FavoriteRateItem` 대응)
public struct FavoriteRateItem: Equatable {
    public let currency: CurrencyInfo
    public let convertedAmount: String
    public let rateLabel: String

    public init(currency: CurrencyInfo, convertedAmount: String, rateLabel: String) {
        self.currency = currency
        self.convertedAmount = convertedAmount
        self.rateLabel = rateLabel
    }
}

public struct ExchangeFailure: Equatable {
    public enum Scope {
        case currencies
        case rate
    }

    public let scope: Scope
    public let message: String

    public init(scope: Scope, message: String) {
        self.scope = scope
        self.message = message
    }
}
