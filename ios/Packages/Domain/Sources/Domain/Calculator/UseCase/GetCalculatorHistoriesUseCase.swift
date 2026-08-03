public struct GetCalculatorHistoriesUseCase: Sendable {
    private let repository: any CalculatorHistoryRepository

    public init(repository: any CalculatorHistoryRepository) {
        self.repository = repository
    }

    public func callAsFunction() -> AsyncStream<[CalculatorHistory]> {
        repository.histories()
    }
}
