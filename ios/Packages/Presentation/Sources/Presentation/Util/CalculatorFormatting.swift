import SwiftUI

/// 계산기 표시용 포맷터 (Android `ThousandSeparatorTransformation`/`formatNumberWithCommas` 대응)
enum CalculatorFormatting {
    private static let operators: Set<Character> = ["+", "-", "×", "÷"]

    /// 수식·결과 문자열의 숫자 부분에 천 단위 쉼표를 넣는다.
    static func formatWithCommas(_ text: String) -> String {
        if text.isEmpty || text == "Error" { return text }

        var result = ""
        var currentNumber = ""

        for char in text {
            if char.isNumber || char == "." {
                currentNumber.append(char)
            } else {
                if !currentNumber.isEmpty {
                    result += formatNumber(currentNumber)
                    currentNumber = ""
                }
                result.append(char)
            }
        }

        if !currentNumber.isEmpty {
            result += formatNumber(currentNumber)
        }

        return result
    }

    /// 천 단위 쉼표를 넣고, 연산자를 강조 색으로 칠한 수식을 만든다.
    /// 연산자 앞에는 줄바꿈 힌트(zero-width space)를 넣는다.
    static func attributedExpression(_ expression: String, operatorColor: Color) -> AttributedString {
        var formatted = ""
        var currentNumber = ""

        for char in expression {
            if char.isNumber || char == "." {
                currentNumber.append(char)
            } else {
                if !currentNumber.isEmpty {
                    formatted += formatNumber(currentNumber)
                    currentNumber = ""
                }
                if operators.contains(char) {
                    formatted.append("\u{200B}")
                }
                formatted.append(char)
            }
        }

        if !currentNumber.isEmpty {
            formatted += formatNumber(currentNumber)
        }

        var attributed = AttributedString(formatted)
        let chars = Array(formatted)

        for (index, char) in chars.enumerated()
            where operators.contains(char) && !isUnaryMinus(chars, index: index) {
            let start = attributed.index(attributed.startIndex, offsetByCharacters: index)
            let end = attributed.index(start, offsetByCharacters: 1)
            attributed[start..<end].foregroundColor = operatorColor
        }

        return attributed
    }

    private static func isUnaryMinus(_ chars: [Character], index: Int) -> Bool {
        guard chars[index] == "-" else { return false }

        let previousChar = chars.prefix(index).last { $0 != "\u{200B}" && $0 != "," }

        return previousChar == nil || previousChar == "(" || operators.contains(previousChar!)
    }

    private static func formatNumber(_ numberString: String) -> String {
        if numberString.contains(".") {
            let parts = numberString.split(separator: ".", omittingEmptySubsequences: false)
            let intPart = groupedInteger(String(parts[0]))

            return parts.count > 1 ? "\(intPart).\(parts[1])" : intPart
        }

        return groupedInteger(numberString)
    }

    private static func groupedInteger(_ digits: String) -> String {
        guard let value = Int64(digits) else { return digits }

        var formatted = String(value)
        var insertIndex = formatted.endIndex

        for _ in 0..<((formatted.count - 1) / 3) {
            insertIndex = formatted.index(insertIndex, offsetBy: -3)
            formatted.insert(",", at: insertIndex)
        }

        return formatted
    }
}
