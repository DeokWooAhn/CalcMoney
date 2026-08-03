public struct SaveExchangeToCurrencyUseCase: Sendable {
    private let repository: any CurrencySelectionRepository

    public init(repository: any CurrencySelectionRepository) {
        self.repository = repository
    }

    public func callAsFunction(_ code: String) async throws {
        try await repository.saveExchangeToCurrencyCode(code)
    }
}
