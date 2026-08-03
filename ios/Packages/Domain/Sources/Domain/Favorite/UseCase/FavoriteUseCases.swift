/// 즐겨찾기 화면에서 쓰는 유스케이스 묶음 (Android `FavoriteUseCases` 대응)
public struct FavoriteUseCases: Sendable {
    public let buildFavoriteRates: BuildFavoriteRatesUseCase
    public let getFavoriteCurrencies: GetFavoriteCurrenciesUseCase
    public let toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase

    public init(
        buildFavoriteRates: BuildFavoriteRatesUseCase,
        getFavoriteCurrencies: GetFavoriteCurrenciesUseCase,
        toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase,
    ) {
        self.buildFavoriteRates = buildFavoriteRates
        self.getFavoriteCurrencies = getFavoriteCurrencies
        self.toggleFavoriteCurrency = toggleFavoriteCurrency
    }
}
