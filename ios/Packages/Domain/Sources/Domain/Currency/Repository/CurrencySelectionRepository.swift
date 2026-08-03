public protocol CurrencySelectionRepository: Sendable {
    func calculatorSelection() async throws -> CalculatorCurrencySelection

    func saveCalculatorMainCurrencyCode(_ code: String) async throws

    func saveCalculatorSubCurrencyCode(_ code: String) async throws

    func saveCalculatorSelection(mainCode: String, subCode: String) async throws

    func exchangeSelection() async throws -> ExchangeCurrencySelection

    func saveExchangeFromCurrencyCode(_ code: String) async throws

    func saveExchangeToCurrencyCode(_ code: String) async throws

    func saveExchangeSelection(fromCode: String, toCode: String) async throws
}
