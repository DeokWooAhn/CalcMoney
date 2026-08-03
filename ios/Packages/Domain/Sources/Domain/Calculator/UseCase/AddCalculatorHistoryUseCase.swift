public struct AddCalculatorHistoryUseCase: Sendable {
    private let repository: any CalculatorHistoryRepository

    public init(repository: any CalculatorHistoryRepository) {
        self.repository = repository
    }

    public func callAsFunction(_ history: CalculatorHistory) async throws {
        try await repository.addHistory(history)
    }
}
