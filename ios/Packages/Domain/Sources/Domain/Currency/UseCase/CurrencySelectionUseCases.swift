/// 통화 선택 관련 유스케이스 묶음 (Android `CurrencySelectionUseCases` 대응)
public struct CurrencySelectionUseCases: Sendable {
    public let getCalculatorSelection: GetCalculatorSelectionUseCase
    public let saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase
    public let saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase
    public let saveCalculatorSelection: SaveCalculatorSelectionUseCase
    public let getExchangeSelection: GetExchangeSelectionUseCase
    public let saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase
    public let saveExchangeToCurrency: SaveExchangeToCurrencyUseCase
    public let saveExchangeSelection: SaveExchangeSelectionUseCase

    public init(
        getCalculatorSelection: GetCalculatorSelectionUseCase,
        saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase,
        saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase,
        saveCalculatorSelection: SaveCalculatorSelectionUseCase,
        getExchangeSelection: GetExchangeSelectionUseCase,
        saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase,
        saveExchangeToCurrency: SaveExchangeToCurrencyUseCase,
        saveExchangeSelection: SaveExchangeSelectionUseCase,
    ) {
        self.getCalculatorSelection = getCalculatorSelection
        self.saveCalculatorMainCurrency = saveCalculatorMainCurrency
        self.saveCalculatorSubCurrency = saveCalculatorSubCurrency
        self.saveCalculatorSelection = saveCalculatorSelection
        self.getExchangeSelection = getExchangeSelection
        self.saveExchangeFromCurrency = saveExchangeFromCurrency
        self.saveExchangeToCurrency = saveExchangeToCurrency
        self.saveExchangeSelection = saveExchangeSelection
    }
}
