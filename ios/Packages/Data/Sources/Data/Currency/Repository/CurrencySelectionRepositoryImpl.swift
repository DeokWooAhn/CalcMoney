import Domain

public struct CurrencySelectionRepositoryImpl: CurrencySelectionRepository {
    private let dataSource: CurrencySelectionDataSource

    public init(dataSource: CurrencySelectionDataSource) {
        self.dataSource = dataSource
    }

    public func calculatorSelection() async throws -> CalculatorCurrencySelection {
        await dataSource.calculatorSelection()
    }

    public func saveCalculatorMainCurrencyCode(_ code: String) async throws {
        await dataSource.saveCalculatorMainCurrencyCode(code)
    }

    public func saveCalculatorSubCurrencyCode(_ code: String) async throws {
        await dataSource.saveCalculatorSubCurrencyCode(code)
    }

    public func saveCalculatorSelection(mainCode: String, subCode: String) async throws {
        await dataSource.saveCalculatorSelection(mainCode: mainCode, subCode: subCode)
    }

    public func exchangeSelection() async throws -> ExchangeCurrencySelection {
        await dataSource.exchangeSelection()
    }

    public func saveExchangeFromCurrencyCode(_ code: String) async throws {
        await dataSource.saveExchangeFromCurrencyCode(code)
    }

    public func saveExchangeToCurrencyCode(_ code: String) async throws {
        await dataSource.saveExchangeToCurrencyCode(code)
    }

    public func saveExchangeSelection(fromCode: String, toCode: String) async throws {
        await dataSource.saveExchangeSelection(fromCode: fromCode, toCode: toCode)
    }
}
