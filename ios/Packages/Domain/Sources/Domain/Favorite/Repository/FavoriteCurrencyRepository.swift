public protocol FavoriteCurrencyRepository: Sendable {
    func favoriteCurrencyCodes() -> AsyncStream<[String]>

    func addFavorite(_ currencyCode: String) async throws

    func removeFavorite(_ currencyCode: String) async throws

    func isFavorite(_ currencyCode: String) async throws -> Bool
}
