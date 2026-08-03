import Foundation

public struct ExtractRepeatOperationUseCase: Sendable {
    private static let operators: Set<Character> = ["+", "-", "−", "×", "÷"]

    public init() {}

    public func callAsFunction(_ expression: String) -> String? {
        let chars = Array(expression)
        let operatorIndex = findLastBinaryOperatorIndex(chars)

        guard operatorIndex > 0, operatorIndex != chars.count - 1 else {
            return nil
        }

        let operatorSymbol = String(chars[operatorIndex])
        let operand = String(chars[(operatorIndex + 1)...])

        guard isNumericOperand(operand) else {
            return nil
        }

        return operatorSymbol + operand
    }

    private func isNumericOperand(_ operand: String) -> Bool {
        Double(operand.replacingOccurrences(of: "−", with: "-")) != nil
    }

    private func findLastBinaryOperatorIndex(_ chars: [Character]) -> Int {
        for index in stride(from: chars.count - 1, through: 0, by: -1) {
            let current = chars[index]

            guard Self.operators.contains(current) else { continue }
            guard index > 0 else { continue }

            let previous = chars[index - 1]

            if !Self.operators.contains(previous), previous != "(" {
                return index
            }
        }
        return -1
    }
}
