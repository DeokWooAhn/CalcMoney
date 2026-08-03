import Foundation

public struct ConvertExchangeAmountUseCase: Sendable {
    private let calculateExpression: CalculateExpressionUseCase

    public init(calculateExpression: CalculateExpressionUseCase) {
        self.calculateExpression = calculateExpression
    }

    /// 수식 안의 숫자들을 환율로 환산하고 연산자는 그대로 유지한 문자열을 반환합니다.
    public func convertExpression(_ expression: String, rate: Double, currencyCode: String?) -> String {
        guard !expression.isEmpty, rate > 0.0, let currencyCode else { return "" }

        var result = ""
        var number = ""

        func flushNumber() {
            guard !number.isEmpty else { return }

            var converted: String?
            if let amount = Double(number), amount.isFinite {
                let value = amount * rate
                if value.isFinite {
                    converted = Self.formatConvertedAmount(value)
                }
            }

            if let converted {
                result += converted + " " + currencyCode
            } else {
                result += number
            }

            number = ""
        }

        for char in expression {
            if char.isNumber || char == "." {
                number.append(char)
            } else {
                flushNumber()
                result += " " + String(char) + " "
            }
        }

        flushNumber()

        return result
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces)
    }

    /// 단일 금액(또는 계산식) 텍스트를 환율로 환산한 문자열을 반환합니다.
    public func convertSingleAmount(_ text: String, rate: Double, currencyCode: String?) -> String {
        guard !text.isEmpty, rate > 0.0, let currencyCode else { return "" }

        let parsedAmount: Double? = Double(text) ?? {
            let calculated = calculateExpression.calculate(text)
            return calculated != "Error" ? Double(calculated) : nil
        }()

        guard let amount = parsedAmount, amount.isFinite else { return "" }

        let convertedAmount = amount * rate

        guard convertedAmount.isFinite else { return "" }

        return "\(Self.formatConvertedAmount(convertedAmount)) \(currencyCode)"
    }

    /// 계산기 환산 결과는 정수로 반올림하지 않고 소수점 둘째 자리까지 표시합니다.
    /// 예를 들어 1,300 KRW를 1 USD = 1,441.10 KRW로 환산하면 0.90 USD가 됩니다.
    private static func formatConvertedAmount(_ amount: Double) -> String {
        String(format: "%.2f", amount)
    }
}
