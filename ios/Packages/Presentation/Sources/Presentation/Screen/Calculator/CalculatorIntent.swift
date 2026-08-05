import Domain

/// 계산기 입력 토큰 (Android `CalculatorToken` 대응)
public enum CalculatorToken: Equatable {
    case number(String)
    case `operator`(String)
    case dot
    /// 괄호는 로직이 복잡하므로 별도 토큰
    case parenthesis
}

/// 계산기 화면 의도 (Android `CalculatorContract.Intent` 대응)
public enum CalculatorIntent: Equatable {
    case input(CalculatorToken)
    case moveCursor(Int)
    case delete
    case clear
    case calculate
    case clearHistory
    case selectMainExchangeCurrency(CurrencyInfo)
    case selectExchangeCurrency(CurrencyInfo)
    case toggleFavorite(String)
    case swapExchangeCurrencies
}
