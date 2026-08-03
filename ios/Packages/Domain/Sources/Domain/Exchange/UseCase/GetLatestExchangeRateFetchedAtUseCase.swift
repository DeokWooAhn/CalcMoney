public struct GetLatestExchangeRateFetchedAtUseCase: Sendable {
    private let repository: any ExchangeRateRepository

    public init(repository: any ExchangeRateRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws -> Int {
        try await repository.latestFetchedAt()
    }
}
