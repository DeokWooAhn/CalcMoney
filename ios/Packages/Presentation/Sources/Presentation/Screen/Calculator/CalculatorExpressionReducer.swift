import Domain

/// 수식 입력 결과 (Android `CalculatorExpressionResult` 대응)
enum CalculatorExpressionResult {
    case updated(CalculatorState)
    case maxNumberLengthExceeded
}

/// 수식 편집 로직을 담당하는 리듀서 (Android `CalculatorExpressionReducer` 대응)
struct CalculatorExpressionReducer {
    static let maxNumberLength = 15
    private static let operators: Set<Character> = ["+", "-", "×", "÷"]
    private static let separators: Set<Character> = ["+", "-", "×", "÷", "(", ")"]

    private let calculateExpression: CalculateExpressionUseCase
    private let convertExchangeAmount: ConvertExchangeAmountUseCase

    init(
        calculateExpression: CalculateExpressionUseCase,
        convertExchangeAmount: ConvertExchangeAmountUseCase,
    ) {
        self.calculateExpression = calculateExpression
        self.convertExchangeAmount = convertExchangeAmount
    }

    func inputNumber(_ currentState: CalculatorState, number: String) -> CalculatorExpressionResult {
        let expression = currentState.expression
        let cursorPosition = currentState.cursorPosition

        if !canInsertNumber(expression, cursorPosition: cursorPosition) {
            return .maxNumberLengthExceeded
        }

        if expression == "0", number != "0" {
            return .updated(
                buildNewExpressionState(currentState: currentState, newExpression: number, newCursorPos: number.count),
            )
        }

        return .updated(buildInsertState(currentState, textToInsert: number))
    }

    func inputOperator(_ currentState: CalculatorState, operatorSymbol: String) -> CalculatorState? {
        // operatorSymbol 은 public 한 CalculatorToken.operator(String) 을 통해 들어오므로 임의 문자열일 수 있다.
        // 아래 치환에서 쓰던 Character(operatorSymbol) 은 빈 문자열이거나 두 글자 이상이면 런타임 트랩이라
        // 여기서 한 글자짜리 연산자인지 먼저 확인하고, 아니면 아무 일도 하지 않는다.
        guard operatorSymbol.count == 1,
              let operatorCharacter = operatorSymbol.first,
              Self.operators.contains(operatorCharacter)
        else {
            return nil
        }

        let chars = Array(currentState.expression)
        let cursorPosition = currentState.cursorPosition

        if chars.isEmpty, operatorSymbol != "-" { return nil }
        if cursorPosition == 0, operatorSymbol != "-" { return nil }

        let charBefore: Character? = cursorPosition > 0 && cursorPosition <= chars.count
            ? chars[cursorPosition - 1]
            : nil

        if let charBefore, Self.operators.contains(charBefore), operatorSymbol == "-", charBefore != "-" {
            return buildInsertState(currentState, textToInsert: operatorSymbol)
        }

        if let charBefore, Self.operators.contains(charBefore) {
            var newChars = chars
            newChars[cursorPosition - 1] = operatorCharacter

            return buildNewExpressionState(
                currentState: currentState,
                newExpression: String(newChars),
                newCursorPos: cursorPosition,
            )
        }

        if charBefore == "(", operatorSymbol != "-" { return nil }

        return buildInsertState(currentState, textToInsert: operatorSymbol)
    }

    func inputDot(_ currentState: CalculatorState) -> CalculatorState? {
        let chars = Array(currentState.expression)
        let cursorPosition = currentState.cursorPosition

        if chars.isEmpty {
            return buildNewExpressionState(currentState: currentState, newExpression: "0.", newCursorPos: 2)
        }

        let currentNumberBlock = currentNumberBlock(chars, cursorPosition: cursorPosition)
        if currentNumberBlock.contains(".") { return nil }

        let charBefore: Character? = cursorPosition > 0 && cursorPosition <= chars.count
            ? chars[cursorPosition - 1]
            : nil

        if let charBefore, Self.operators.contains(charBefore) || charBefore == "(" {
            return buildInsertState(currentState, textToInsert: "0.")
        }

        return buildInsertState(currentState, textToInsert: ".")
    }

    func inputParenthesis(_ currentState: CalculatorState) -> CalculatorState {
        let chars = Array(currentState.expression)
        let cursorPosition = currentState.cursorPosition
        let openCount = chars.count(where: { $0 == "(" })
        let closeCount = chars.count(where: { $0 == ")" })
        let charBefore: Character? = cursorPosition > 0 && cursorPosition <= chars.count
            ? chars[cursorPosition - 1]
            : nil

        let textToInsert: String = if openCount > closeCount, let charBefore, charBefore.isNumber || charBefore == ")" {
            ")"
        } else if let charBefore, charBefore.isNumber || charBefore == ")" {
            "×("
        } else {
            "("
        }

        return buildInsertState(currentState, textToInsert: textToInsert)
    }

    func delete(_ currentState: CalculatorState) -> CalculatorState? {
        var chars = Array(currentState.expression)
        let cursorPosition = currentState.cursorPosition

        if chars.isEmpty || cursorPosition == 0 { return nil }

        chars.remove(at: cursorPosition - 1)

        return buildNewExpressionState(
            currentState: currentState,
            newExpression: String(chars),
            newCursorPos: cursorPosition - 1,
        )
    }

    func buildNewExpressionState(
        currentState: CalculatorState,
        newExpression: String,
        newCursorPos: Int,
    ) -> CalculatorState {
        var state = currentState
        state.expression = newExpression
        state.cursorPosition = newCursorPos
        state.previewResult = calculatePreview(newExpression)
        state.repeatOperation = nil
        state.isCalculatedResult = false
        state.isError = false
        state.errorMessage = nil

        return withConvertedAmounts(state)
    }

    func hasBinaryOperator(_ expression: String) -> Bool {
        let chars = Array(expression)

        return chars.indices.contains { index in
            Self.operators.contains(chars[index]) && !isUnaryMinusOperator(chars, index: index)
        }
    }

    func withConvertedAmounts(_ state: CalculatorState) -> CalculatorState {
        var state = state
        state.convertedExpressionAmount = convertExchangeAmount.convertExpression(
            state.expression,
            rate: state.exchangeRate,
            currencyCode: state.selectedExchangeCurrency?.code,
        )
        state.convertedPreviewAmount = convertExchangeAmount.convertSingleAmount(
            state.previewResult,
            rate: state.exchangeRate,
            currencyCode: state.selectedExchangeCurrency?.code,
        )

        return state
    }

    private func buildInsertState(_ currentState: CalculatorState, textToInsert: String) -> CalculatorState {
        var chars = Array(currentState.expression)
        chars.insert(contentsOf: textToInsert, at: currentState.cursorPosition)

        return buildNewExpressionState(
            currentState: currentState,
            newExpression: String(chars),
            newCursorPos: currentState.cursorPosition + textToInsert.count,
        )
    }

    private func calculatePreview(_ expression: String) -> String {
        guard let lastChar = expression.last else { return "" }

        if !hasBinaryOperator(expression) || Self.operators.contains(lastChar) {
            return ""
        }

        let result = calculateExpression.calculate(expression)

        return result == "Error" ? "" : result
    }

    private func isUnaryMinusOperator(_ chars: [Character], index: Int) -> Bool {
        guard chars[index] == "-" else { return false }
        guard index > 0 else { return true }

        let previousChar = chars[index - 1]

        return previousChar == "(" || Self.operators.contains(previousChar)
    }

    private func canInsertNumber(_ expression: String, cursorPosition: Int) -> Bool {
        currentNumberBlock(Array(expression), cursorPosition: cursorPosition).count < Self.maxNumberLength
    }

    private func currentNumberBlock(_ chars: [Character], cursorPosition: Int) -> String {
        if chars.isEmpty { return "" }

        let beforeCursor = chars.prefix(cursorPosition)
        let lastSeparatorIndex = beforeCursor.lastIndex(where: { Self.separators.contains($0) })
        let startOfNumber = lastSeparatorIndex.map { $0 + 1 } ?? 0

        let afterCursor = chars.suffix(from: min(cursorPosition, chars.count))
        let firstSeparatorIndex = afterCursor.firstIndex(where: { Self.separators.contains($0) })
        let endOfNumber = firstSeparatorIndex ?? chars.count

        return String(chars[startOfNumber..<endOfNumber])
    }
}
