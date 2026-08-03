/// 계산기 기록 (Android `CalculatorHistory` 대응)
public struct CalculatorHistory: Equatable, Hashable, Sendable {
    public let expression: String
    public let result: String

    public init(expression: String, result: String) {
        self.expression = expression
        self.result = result
    }
}
