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

extension CalculatorKey {
    /// E2E 테스트(Maestro)가 키를 찾을 때 쓰는 id. Android `CalculatorKey.testTag`와 값이 같아야
    /// Flow 하나로 두 플랫폼을 돌릴 수 있다.
    ///
    /// 숫자는 그대로, 나머지는 영어 이름으로 짓고 기호(`+`, `=`, `( )`)는 쓰지 않는다. Maestro는 `id:`를
    /// 정규식으로 해석해서, `keypad.+`는 "keypad 뒤에 아무 글자나"가 되어 다른 키를 누른다.
    var testID: String {
        let name = switch self {
        case .history: "history"
        case .clear: "clear"
        case .parenthesis: "parenthesis"
        case .dot: "dot"
        case .delete: "delete"
        case .calculate: "equals"
        case let .number(value): value
        case let .operatorKey(_, inputValue):
            switch inputValue {
            case "+": "plus"
            case "-": "minus"
            case "×": "multiply"
            case "÷": "divide"
            default: inputValue
            }
        }

        return "keypad.\(name)"
    }
}
