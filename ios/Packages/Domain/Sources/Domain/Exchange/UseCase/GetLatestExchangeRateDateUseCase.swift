public struct GetLatestExchangeRateDateUseCase: Sendable {
    private let repository: any ExchangeRateRepository

    public init(repository: any ExchangeRateRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws -> String {
        try await repository.latestRateDate()
    }
}
