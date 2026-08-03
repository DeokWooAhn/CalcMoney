public struct GetFavoriteCurrenciesUseCase: Sendable {
    private let repository: any FavoriteCurrencyRepository

    public init(repository: any FavoriteCurrencyRepository) {
        self.repository = repository
    }

    public func callAsFunction() -> AsyncStream<[String]> {
        repository.favoriteCurrencyCodes()
    }
}
