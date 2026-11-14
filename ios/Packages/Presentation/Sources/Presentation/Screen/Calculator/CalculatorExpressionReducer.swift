import Domain

/// 수식 입력 결과 (Android `CalculatorExpressionResult` 대응)
enum CalculatorExpressionResult {
    case updated(CalculatorState)
    case maxNumberLengthExceeded
}

/// "=" 계산 결과 (Android `CalculationOutcome` 대응). 화면에 반영할 상태와, 성공했을 때 저장할 기록을 담는다.
enum CalculationOutcome: Equatable {
    /// 계산할 수식이 아니다. 상태를 바꾸지 않는다.
    case ignored
    /// 계산할 수 없는 수식이다. 오류 상태를 보여 주고 기록에 남기지 않는다.
    case failed(CalculatorState)
    /// 계산에 성공했다. 상태를 바로 보여 주고, 기록은 저장에 성공한 뒤 기록 목록에 넣는다.
    case succeeded(CalculatorState, history: CalculatorState.HistoryItem)
}

/// 수식 편집 로직을 담당하는 리듀서 (Android `CalculatorExpressionReducer` 대응)
struct CalculatorExpressionReducer {
    static let maxNumberLength = 15
    private static let operators: Set<Character> = ["+", "-", "×", "÷"]
    private static let separators: Set<Character> = ["+", "-", "×", "÷", "(", ")"]

    private let calculateExpression: CalculateExpressionUseCase
    private let convertExchangeAmount: ConvertExchangeAmountUseCase
    private let extractRepeatOperation: ExtractRepeatOperationUseCase

    init(
        calculateExpression: CalculateExpressionUseCase,
        convertExchangeAmount: ConvertExchangeAmountUseCase,
        extractRepeatOperation: ExtractRepeatOperationUseCase,
    ) {
        self.calculateExpression = calculateExpression
        self.convertExchangeAmount = convertExchangeAmount
        self.extractRepeatOperation = extractRepeatOperation
    }

    /// "="를 눌렀을 때의 다음 상태와 저장할 기록을 정한다 (Android `CalculatorExpressionReducer.calculate` 대응)
    ///
    /// 상태를 바꾸거나 저장하지 않는 순수 함수다. 숫자만 남은 결과에서 다시 누르면 직전 연산(`repeatOperation`)을
    /// 이어 붙여 계산한다.
    func calculate(_ currentState: CalculatorState) -> CalculationOutcome {
        let expression = currentState.expression
        guard !expression.isEmpty else { return .ignored }

        let repeatOperation = currentState.repeatOperation
        let shouldRepeatOperation = repeatOperation != nil && Double(expression) != nil
        if !shouldRepeatOperation, !hasBinaryOperator(expression) { return .ignored }

        let expressionToCalculate = shouldRepeatOperation ? expression + (repeatOperation ?? "") : expression
        let result = calculateExpression.calculate(expressionToCalculate)

        if result == "Error" {
            var failedState = currentState
            failedState.isError = true
            failedState.errorMessage = L("계산 오류")
            return .failed(failedState)
        }

        var nextState = buildNewExpressionState(
            currentState: currentState,
            newExpression: result,
            newCursorPos: result.count,
        )
        nextState.previewResult = ""
        nextState.convertedPreviewAmount = ""
        nextState.repeatOperation = extractRepeatOperation(expressionToCalculate) ?? repeatOperation
        nextState.isCalculatedResult = true

        return .succeeded(
            nextState,
            history: CalculatorState.HistoryItem(expression: expressionToCalculate, result: result),
        )
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
            Self.operators.contains(chars[index])
                && !isUnaryMinusOperator(chars, index: index)
                && !isExponentSign(chars, index: index)
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

    /// 계산 결과 1.0000000000E+16의 +는 숫자의 일부라 이항 연산자로 세지 않는다.
    private func isExponentSign(_ chars: [Character], index: Int) -> Bool {
        guard chars[index] == "+" || chars[index] == "-", index > 0 else { return false }

        return chars[index - 1] == "E"
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
