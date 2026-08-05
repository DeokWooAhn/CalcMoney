import Domain

/// 계산기 화면 상태 (Android `CalculatorContract.State` 대응)
public struct CalculatorState: Equatable {
    public struct HistoryItem: Equatable {
        public let expression: String
        public let result: String

        public init(expression: String, result: String) {
            self.expression = expression
            self.result = result
        }
    }

    public var expression = ""
    /// 커서 위치를 별도 관리해야 중간 편집 가능
    public var cursorPosition = 0
    public var previewResult = ""
    public var mainExchangeCurrency: CurrencyInfo?
    public var selectedExchangeCurrency: CurrencyInfo?
    public var availableCurrencies: [CurrencyInfo] = []
    public var favoriteCurrencyCodes: [String] = []
    public var exchangeRate = 0.0
    public var convertedExpressionAmount = ""
    public var convertedPreviewAmount = ""
    public var isCalculatedResult = false
    /// 마지막 연산자 저장 (반복 계산용)
    public var repeatOperation: String?
    public var histories: [HistoryItem] = []
    public var isError = false
    public var errorMessage: String?

    public init() {}
}
