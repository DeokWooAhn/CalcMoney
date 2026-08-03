public struct GetExchangeSelectionUseCase: Sendable {
    private let repository: any CurrencySelectionRepository

    public init(repository: any CurrencySelectionRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws -> ExchangeCurrencySelection {
        try await repository.exchangeSelection()
    }
}
