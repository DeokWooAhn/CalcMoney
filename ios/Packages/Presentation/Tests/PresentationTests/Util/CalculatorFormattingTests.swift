import Testing
@testable import Presentation

@Suite("CalculatorFormatting")
struct CalculatorFormattingTests {
    @Test("소수 둘째 자리 금액은 정수부에만 세 자리마다 쉼표를 넣는다", arguments: [
        ("1343.87", "1,343.87"),
        ("1000000.00", "1,000,000.00"),
        ("12000.00", "12,000.00"),
    ])
    func 금액에_천_단위_쉼표를_넣는다(input: String, expected: String) {
        #expect(CalculatorFormatting.formatWithCommas(input) == expected)
    }

    @Test("천 미만 금액과 빈 금액 대신 쓰는 0은 그대로 둔다", arguments: ["999.99", "0", ""])
    func 천_미만_금액은_그대로_둔다(input: String) {
        #expect(CalculatorFormatting.formatWithCommas(input) == input)
    }
}
