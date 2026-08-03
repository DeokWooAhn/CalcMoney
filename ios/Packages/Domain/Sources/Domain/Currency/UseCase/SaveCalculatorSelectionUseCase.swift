public struct SaveCalculatorSelectionUseCase: Sendable {
    private let repository: any CurrencySelectionRepository

    public init(repository: any CurrencySelectionRepository) {
        self.repository = repository
    }

    public func callAsFunction(mainCode: String, subCode: String) async throws {
        try await repository.saveCalculatorSelection(mainCode: mainCode, subCode: subCode)
    }
}
