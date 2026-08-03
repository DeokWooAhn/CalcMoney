public struct ToggleFavoriteCurrencyUseCase: Sendable {
    private let repository: any FavoriteCurrencyRepository

    public init(repository: any FavoriteCurrencyRepository) {
        self.repository = repository
    }

    public func callAsFunction(_ currencyCode: String) async throws {
        if try await repository.isFavorite(currencyCode) {
            try await repository.removeFavorite(currencyCode)
        } else {
            try await repository.addFavorite(currencyCode)
        }
    }
}
