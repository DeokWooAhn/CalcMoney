import Data
import Domain
import Presentation

/// 앱 전체 의존성을 조립하는 수동 DI 컨테이너 (Android의 Hilt 모듈 대응)
///
/// Data의 Repository 구현체 → Domain UseCase → Presentation ViewModel 순으로 조립한다.
final class AppContainer {
    let calculatorViewModel: CalculatorViewModel
    /// Android의 activity-scoped 공유처럼 Exchange·Favorite·Setting 탭이 함께 쓴다.
    let exchangeViewModel: ExchangeViewModel
    let favoriteViewModel: FavoriteViewModel
    let mainViewModel: MainViewModel
    let adConsentManager = AdConsentManager()

    init() {
        let repositories = Repositories()
        let useCases = UseCases(repositories: repositories)

        calculatorViewModel = CalculatorViewModel(
            calculatorUseCases: useCases.calculator,
            exchangeUseCases: useCases.exchange,
            favoriteUseCases: useCases.favorite,
            currencySelectionUseCases: useCases.currencySelection,
        )
        exchangeViewModel = ExchangeViewModel(
            exchangeUseCases: useCases.exchange,
            favoriteUseCases: useCases.favorite,
            currencySelectionUseCases: useCases.currencySelection,
        )
        favoriteViewModel = FavoriteViewModel(
            exchangeUseCases: useCases.exchange,
            favoriteUseCases: useCases.favorite,
        )
        mainViewModel = MainViewModel(themeUseCases: useCases.theme)
    }
}

/// Data 레이어 구현체 모음 (Android `RepositoryModule` 대응)
private struct Repositories {
    let exchangeRate: any ExchangeRateRepository
    let currencySelection: any CurrencySelectionRepository
    let favoriteCurrency: any FavoriteCurrencyRepository
    let theme: any ThemeRepository
    let calculatorHistory: any CalculatorHistoryRepository

    init() {
        // GoogleService-Info.plist가 없으면 원격 환율은 "준비 안 됨" 상태로 동작한다.
        let isFirebaseConfigured = FirebaseBootstrap.configureIfAvailable()
        let remoteDataSource: any ExchangeRateRemoteDataSource = isFirebaseConfigured
            ? FirestoreExchangeRateDataSource()
            : UnavailableExchangeRateRemoteDataSource()

        exchangeRate = ExchangeRateRepositoryImpl(
            remoteDataSource: remoteDataSource,
            localDataSource: Self.makeExchangeRateLocalDataSource(),
        )
        currencySelection = CurrencySelectionRepositoryImpl(dataSource: CurrencySelectionDataSource())
        favoriteCurrency = FavoriteCurrencyRepositoryImpl(dataSource: FavoriteCurrencyDataSource())
        theme = ThemeRepositoryImpl(dataSource: ThemePreferenceDataSource())
        calculatorHistory = CalculatorHistoryRepositoryImpl(dataSource: CalculatorHistoryDataSource())
    }

    /// 로컬 캐시 저장소를 만들지 못하면 앱이 정상 동작할 수 없으므로 즉시 중단한다 (Android Room과 동일).
    private static func makeExchangeRateLocalDataSource() -> ExchangeRateLocalDataSource {
        do {
            return try ExchangeRateLocalDataSource.make()
        } catch {
            fatalError("Failed to create the exchange rate cache store: \(error)")
        }
    }
}

/// Domain 유스케이스 묶음 모음 (Android의 UseCases 그룹 대응)
private struct UseCases {
    let calculator: CalculatorUseCases
    let exchange: ExchangeUseCases
    let favorite: FavoriteUseCases
    let currencySelection: CurrencySelectionUseCases
    let theme: ThemeUseCases

    init(repositories: Repositories) {
        let calculateExpression = CalculateExpressionUseCase()

        calculator = CalculatorUseCases(
            addHistory: AddCalculatorHistoryUseCase(repository: repositories.calculatorHistory),
            calculateExpression: calculateExpression,
            clearHistory: ClearCalculatorHistoriesUseCase(repository: repositories.calculatorHistory),
            extractRepeatOperation: ExtractRepeatOperationUseCase(),
            getHistory: GetCalculatorHistoriesUseCase(repository: repositories.calculatorHistory),
        )
        exchange = ExchangeUseCases(
            exchangeAmount: CalculateExchangeAmountUseCase(),
            convertExchangeAmount: ConvertExchangeAmountUseCase(calculateExpression: calculateExpression),
            getExchangeRate: GetExchangeRateUseCase(repository: repositories.exchangeRate),
            getLatestRateDate: GetLatestExchangeRateDateUseCase(repository: repositories.exchangeRate),
            getLatestFetchedAt: GetLatestExchangeRateFetchedAtUseCase(repository: repositories.exchangeRate),
            refreshExchangeRates: RefreshExchangeRatesUseCase(repository: repositories.exchangeRate),
            getSupportedCurrencies: GetSupportedCurrenciesUseCase(repository: repositories.exchangeRate),
        )
        favorite = FavoriteUseCases(
            buildFavoriteRates: BuildFavoriteRatesUseCase(),
            getFavoriteCurrencies: GetFavoriteCurrenciesUseCase(repository: repositories.favoriteCurrency),
            toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase(repository: repositories.favoriteCurrency),
        )
        currencySelection = CurrencySelectionUseCases(
            getCalculatorSelection: GetCalculatorSelectionUseCase(repository: repositories.currencySelection),
            saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase(repository: repositories.currencySelection),
            saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase(repository: repositories.currencySelection),
            saveCalculatorSelection: SaveCalculatorSelectionUseCase(repository: repositories.currencySelection),
            getExchangeSelection: GetExchangeSelectionUseCase(repository: repositories.currencySelection),
            saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase(repository: repositories.currencySelection),
            saveExchangeToCurrency: SaveExchangeToCurrencyUseCase(repository: repositories.currencySelection),
            saveExchangeSelection: SaveExchangeSelectionUseCase(repository: repositories.currencySelection),
        )
        theme = ThemeUseCases(
            getThemeMode: GetThemeModeUseCase(repository: repositories.theme),
            saveThemeMode: SaveThemeModeUseCase(repository: repositories.theme),
        )
    }
}
