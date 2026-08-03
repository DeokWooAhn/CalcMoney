public struct RefreshExchangeRatesUseCase: Sendable {
    private let repository: any ExchangeRateRepository

    public init(repository: any ExchangeRateRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws {
        try await repository.refreshExchangeRates()
    }
}
