import Foundation

public struct CalculateExchangeAmountUseCase: Sendable {
    public init() {}

    public func callAsFunction(fromAmount: String, rate: Double) -> String {
        let amount = Double(fromAmount) ?? 0.0
        let result = amount * rate

        return if result > 0 {
            String(format: "%.2f", result)
        } else {
            ""
        }
    }
}
