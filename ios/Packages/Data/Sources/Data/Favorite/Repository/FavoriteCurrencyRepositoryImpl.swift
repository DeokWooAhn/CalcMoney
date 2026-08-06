import Domain

public struct FavoriteCurrencyRepositoryImpl: FavoriteCurrencyRepository {
    private let dataSource: FavoriteCurrencyDataSource

    public init(dataSource: FavoriteCurrencyDataSource) {
        self.dataSource = dataSource
    }

    public func favoriteCurrencyCodes() -> AsyncStream<[String]> {
        dataSource.favoriteCodes()
    }

    public func addFavorite(_ currencyCode: String) async throws {
        await dataSource.addFavorite(currencyCode)
    }

    public func removeFavorite(_ currencyCode: String) async throws {
        await dataSource.removeFavorite(currencyCode)
    }

    public func isFavorite(_ currencyCode: String) async throws -> Bool {
        await dataSource.isFavorite(currencyCode)
    }
}
