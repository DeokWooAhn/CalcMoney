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

        let chars = Array(expression)
        var index = 0
        while index < chars.count {
            let char = chars[index]

            if char.isNumber || char == "." {
                number.append(char)
                index += 1
            } else if char == "E", !number.isEmpty, let end = Self.exponentEnd(chars, start: index) {
                // 큰·작은 계산 결과(1.2193263111E+17)는 지수부까지 한 숫자다. 지수의 부호를 연산자로 나누면
                // "0.00 USD E + 0.01 USD"처럼 조각마다 환산한다.
                number += String(chars[index..<end])
                index = end
            } else {
                flushNumber()
                result += " " + String(char) + " "
                index += 1
            }
        }

        flushNumber()

        return result
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces)
    }

    /// `start`의 `E`부터 지수 숫자 끝까지의 끝 위치. 지수 숫자가 없으면 nil.
    /// 계산 엔진(`CalculateExpressionUseCase`의 `parseExponent`)과 같은 규칙이다: `E`, 부호(선택), 숫자 한 자리 이상.
    private static func exponentEnd(_ chars: [Character], start: Int) -> Int? {
        var index = start + 1
        if index < chars.count, chars[index] == "+" || chars[index] == "-" { index += 1 }
        let digitsStart = index
        while index < chars.count, chars[index].isNumber { index += 1 }

        return index > digitsStart ? index : nil
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
