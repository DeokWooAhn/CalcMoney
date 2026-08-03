public struct GetExchangeRateUseCase: Sendable {
    private let repository: any ExchangeRateRepository

    public init(repository: any ExchangeRateRepository) {
        self.repository = repository
    }

    public func callAsFunction(from: String, to: String) async throws -> Double {
        try await repository.exchangeRate(from: from, to: to)
    }
}
