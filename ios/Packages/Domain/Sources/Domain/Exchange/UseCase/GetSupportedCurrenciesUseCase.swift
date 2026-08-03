public struct GetSupportedCurrenciesUseCase: Sendable {
    private let repository: any ExchangeRateRepository

    public init(repository: any ExchangeRateRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws -> [CurrencyInfo] {
        try await repository.supportedCurrencies()
    }
}
