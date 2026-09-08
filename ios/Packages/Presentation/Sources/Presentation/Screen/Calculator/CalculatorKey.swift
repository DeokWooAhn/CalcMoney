/// 계산기 키 정의 (Android `CalculatorKey` 대응)
enum CalculatorKey: Hashable {
    case history
    case clear
    case parenthesis
    case dot
    case delete
    case calculate
    case number(String)
    case operatorKey(displayText: String, inputValue: String)
}

let calculatorKeyRows: [[CalculatorKey]] = [
    [.history, .clear, .parenthesis, .operatorKey(displayText: "÷", inputValue: "÷")],
    [.number("7"), .number("8"), .number("9"), .operatorKey(displayText: "×", inputValue: "×")],
    [.number("4"), .number("5"), .number("6"), .operatorKey(displayText: "−", inputValue: "-")],
    [.number("1"), .number("2"), .number("3"), .operatorKey(displayText: "+", inputValue: "+")],
    [.dot, .number("0"), .delete, .calculate],
]

extension CalculatorKey {
    func toIntent() -> CalculatorIntent? {
        switch self {
        case .history:
            nil

        case .clear:
            .clear

        case .parenthesis:
            .input(.parenthesis)

        case .dot:
            .input(.dot)

        case .delete:
            .delete

        case .calculate:
            .calculate

        case let .number(value):
            .input(.number(value))

        case let .operatorKey(_, inputValue):
            .input(.operator(inputValue))
        }
    }
}
