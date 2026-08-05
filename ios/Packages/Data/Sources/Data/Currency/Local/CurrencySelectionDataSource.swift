import Domain
import Foundation

/// 화면별 통화 선택을 UserDefaults에 저장하는 데이터소스 (Android `CurrencySelectionDataSource` 대응)
public actor CurrencySelectionDataSource {
    private static let calculatorMainCurrencyCodeKey = "calculator_main_currency_code"
    private static let calculatorSubCurrencyCodeKey = "calculator_sub_currency_code"
    private static let exchangeFromCurrencyCodeKey = "exchange_from_currency_code"
    private static let exchangeToCurrencyCodeKey = "exchange_to_currency_code"

    private let userDefaults: UserDefaults

    public init(userDefaults: UserDefaults = .suite(named: "currency_selection")) {
        self.userDefaults = userDefaults
    }

    public func calculatorSelection() -> CalculatorCurrencySelection {
        CalculatorCurrencySelection(
            mainCode: userDefaults.string(forKey: Self.calculatorMainCurrencyCodeKey),
            subCode: userDefaults.string(forKey: Self.calculatorSubCurrencyCodeKey),
        )
    }

    public func saveCalculatorMainCurrencyCode(_ code: String) {
        userDefaults.set(code, forKey: Self.calculatorMainCurrencyCodeKey)
    }

    public func saveCalculatorSubCurrencyCode(_ code: String) {
        userDefaults.set(code, forKey: Self.calculatorSubCurrencyCodeKey)
    }

    public func saveCalculatorSelection(mainCode: String, subCode: String) {
        userDefaults.set(mainCode, forKey: Self.calculatorMainCurrencyCodeKey)
        userDefaults.set(subCode, forKey: Self.calculatorSubCurrencyCodeKey)
    }

    public func exchangeSelection() -> ExchangeCurrencySelection {
        ExchangeCurrencySelection(
            fromCode: userDefaults.string(forKey: Self.exchangeFromCurrencyCodeKey),
            toCode: userDefaults.string(forKey: Self.exchangeToCurrencyCodeKey),
        )
    }

    public func saveExchangeFromCurrencyCode(_ code: String) {
        userDefaults.set(code, forKey: Self.exchangeFromCurrencyCodeKey)
    }

    public func saveExchangeToCurrencyCode(_ code: String) {
        userDefaults.set(code, forKey: Self.exchangeToCurrencyCodeKey)
    }

    public func saveExchangeSelection(fromCode: String, toCode: String) {
        userDefaults.set(fromCode, forKey: Self.exchangeFromCurrencyCodeKey)
        userDefaults.set(toCode, forKey: Self.exchangeToCurrencyCodeKey)
    }
}
