/// 계산기 화면에서 쓰는 유스케이스 묶음 (Android `CalculatorUseCases` 대응)
public struct CalculatorUseCases: Sendable {
    public let addHistory: AddCalculatorHistoryUseCase
    public let calculateExpression: CalculateExpressionUseCase
    public let clearHistory: ClearCalculatorHistoriesUseCase
    public let extractRepeatOperation: ExtractRepeatOperationUseCase
    public let getHistory: GetCalculatorHistoriesUseCase

    public init(
        addHistory: AddCalculatorHistoryUseCase,
        calculateExpression: CalculateExpressionUseCase,
        clearHistory: ClearCalculatorHistoriesUseCase,
        extractRepeatOperation: ExtractRepeatOperationUseCase,
        getHistory: GetCalculatorHistoriesUseCase,
    ) {
        self.addHistory = addHistory
        self.calculateExpression = calculateExpression
        self.clearHistory = clearHistory
        self.extractRepeatOperation = extractRepeatOperation
        self.getHistory = getHistory
    }
}
