public struct ClearCalculatorHistoriesUseCase: Sendable {
    private let repository: any CalculatorHistoryRepository

    public init(repository: any CalculatorHistoryRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws {
        try await repository.clearHistories()
    }
}
