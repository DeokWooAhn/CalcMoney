import Foundation
import Testing
@testable import Data

@Suite("CurrencySelectionDataSource")
struct CurrencySelectionDataSourceTests {
    private func makeDataSource() -> CurrencySelectionDataSource {
        let suiteName = "test-\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)

        return CurrencySelectionDataSource(userDefaults: userDefaults)
    }

    @Test("저장된 선택이 없으면 nil을 반환한다")
    func 저장된_선택이_없으면_nil() async {
        let dataSource = makeDataSource()

        let calculator = await dataSource.calculatorSelection()
        let exchange = await dataSource.exchangeSelection()

        #expect(calculator.mainCode == nil)
        #expect(calculator.subCode == nil)
        #expect(exchange.fromCode == nil)
        #expect(exchange.toCode == nil)
    }

    @Test("계산기 통화 선택을 저장하고 다시 읽는다")
    func 계산기_통화_선택_저장() async {
        let dataSource = makeDataSource()

        await dataSource.saveCalculatorSelection(mainCode: "KRW", subCode: "USD")

        let selection = await dataSource.calculatorSelection()

        #expect(selection.mainCode == "KRW")
        #expect(selection.subCode == "USD")
    }

    @Test("환율 화면 통화를 개별 키로 저장한다")
    func 환율_화면_통화_개별_저장() async {
        let dataSource = makeDataSource()

        await dataSource.saveExchangeFromCurrencyCode("USD")
        await dataSource.saveExchangeToCurrencyCode("KRW")

        let selection = await dataSource.exchangeSelection()

        #expect(selection.fromCode == "USD")
        #expect(selection.toCode == "KRW")
    }
}
