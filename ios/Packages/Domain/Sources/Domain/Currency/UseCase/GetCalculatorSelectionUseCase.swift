public struct GetCalculatorSelectionUseCase: Sendable {
    private let repository: any CurrencySelectionRepository

    public init(repository: any CurrencySelectionRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws -> CalculatorCurrencySelection {
        try await repository.calculatorSelection()
    }
}
