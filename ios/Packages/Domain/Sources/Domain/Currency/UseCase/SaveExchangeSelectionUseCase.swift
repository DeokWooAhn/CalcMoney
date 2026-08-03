public struct SaveExchangeSelectionUseCase: Sendable {
    private let repository: any CurrencySelectionRepository

    public init(repository: any CurrencySelectionRepository) {
        self.repository = repository
    }

    public func callAsFunction(fromCode: String, toCode: String) async throws {
        try await repository.saveExchangeSelection(fromCode: fromCode, toCode: toCode)
    }
}
