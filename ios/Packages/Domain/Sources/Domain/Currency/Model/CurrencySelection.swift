/// 계산기 화면의 통화 선택 상태 (Android `CalculatorCurrencySelection` 대응)
public struct CalculatorCurrencySelection: Equatable, Sendable {
    public let mainCode: String?
    public let subCode: String?

    public init(mainCode: String?, subCode: String?) {
        self.mainCode = mainCode
        self.subCode = subCode
    }
}

/// 환율 화면의 통화 선택 상태 (Android `ExchangeCurrencySelection` 대응)
public struct ExchangeCurrencySelection: Equatable, Sendable {
    public let fromCode: String?
    public let toCode: String?

    public init(fromCode: String?, toCode: String?) {
        self.fromCode = fromCode
        self.toCode = toCode
    }
}
