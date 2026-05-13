package com.ahn.domain.calculator.usecase

import javax.inject.Inject

class CalculatorUseCases @Inject constructor(
    val addHistory: AddCalculatorHistoryUseCase,
    val calculateExpression: CalculateExpressionUseCase,
    val clearHistory: ClearCalculatorHistoriesUseCase,
    val extractRepeatOperation: ExtractRepeatOperationUseCase,
    val getHistory: GetCalculatorHistoriesUseCase,
)
