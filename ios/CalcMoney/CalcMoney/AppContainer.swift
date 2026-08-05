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
        // GoogleService-Info.plist가 없으면 원격 환율은 "준비 안 됨" 상태로 동작한다.
        let isFirebaseConfigured = FirebaseBootstrap.configureIfAvailable()

        let remoteDataSource: any ExchangeRateRemoteDataSource = isFirebaseConfigured
            ? FirestoreExchangeRateDataSource()
            : UnavailableExchangeRateRemoteDataSource()

        // 로컬 캐시 저장소를 못 만들면 앱이 정상 동작할 수 없으므로 즉시 종료한다 (Android Room과 동일).
        let localDataSource = try! ExchangeRateLocalDataSource.make()

        let exchangeRateRepository = ExchangeRateRepositoryImpl(
            remoteDataSource: remoteDataSource,
            localDataSource: localDataSource,
        )
        let currencySelectionRepository = CurrencySelectionRepositoryImpl(
            dataSource: CurrencySelectionDataSource(),
        )
        let favoriteCurrencyRepository = FavoriteCurrencyRepositoryImpl(
            dataSource: FavoriteCurrencyDataSource(),
        )
        let themeRepository = ThemeRepositoryImpl(dataSource: ThemePreferenceDataSource())
        let calculatorHistoryRepository = CalculatorHistoryRepositoryImpl(
            dataSource: CalculatorHistoryDataSource(),
        )

        let calculateExpression = CalculateExpressionUseCase()
        let calculatorUseCases = CalculatorUseCases(
            addHistory: AddCalculatorHistoryUseCase(repository: calculatorHistoryRepository),
            calculateExpression: calculateExpression,
            clearHistory: ClearCalculatorHistoriesUseCase(repository: calculatorHistoryRepository),
            extractRepeatOperation: ExtractRepeatOperationUseCase(),
            getHistory: GetCalculatorHistoriesUseCase(repository: calculatorHistoryRepository),
        )
        let exchangeUseCases = ExchangeUseCases(
            exchangeAmount: CalculateExchangeAmountUseCase(),
            convertExchangeAmount: ConvertExchangeAmountUseCase(calculateExpression: calculateExpression),
            getExchangeRate: GetExchangeRateUseCase(repository: exchangeRateRepository),
            getLatestRateDate: GetLatestExchangeRateDateUseCase(repository: exchangeRateRepository),
            getLatestFetchedAt: GetLatestExchangeRateFetchedAtUseCase(repository: exchangeRateRepository),
            refreshExchangeRates: RefreshExchangeRatesUseCase(repository: exchangeRateRepository),
            getSupportedCurrencies: GetSupportedCurrenciesUseCase(repository: exchangeRateRepository),
        )
        let favoriteUseCases = FavoriteUseCases(
            buildFavoriteRates: BuildFavoriteRatesUseCase(),
            getFavoriteCurrencies: GetFavoriteCurrenciesUseCase(repository: favoriteCurrencyRepository),
            toggleFavoriteCurrency: ToggleFavoriteCurrencyUseCase(repository: favoriteCurrencyRepository),
        )
        let currencySelectionUseCases = CurrencySelectionUseCases(
            getCalculatorSelection: GetCalculatorSelectionUseCase(repository: currencySelectionRepository),
            saveCalculatorMainCurrency: SaveCalculatorMainCurrencyUseCase(repository: currencySelectionRepository),
            saveCalculatorSubCurrency: SaveCalculatorSubCurrencyUseCase(repository: currencySelectionRepository),
            saveCalculatorSelection: SaveCalculatorSelectionUseCase(repository: currencySelectionRepository),
            getExchangeSelection: GetExchangeSelectionUseCase(repository: currencySelectionRepository),
            saveExchangeFromCurrency: SaveExchangeFromCurrencyUseCase(repository: currencySelectionRepository),
            saveExchangeToCurrency: SaveExchangeToCurrencyUseCase(repository: currencySelectionRepository),
            saveExchangeSelection: SaveExchangeSelectionUseCase(repository: currencySelectionRepository),
        )
        let themeUseCases = ThemeUseCases(
            getThemeMode: GetThemeModeUseCase(repository: themeRepository),
            saveThemeMode: SaveThemeModeUseCase(repository: themeRepository),
        )

        calculatorViewModel = CalculatorViewModel(
            calculatorUseCases: calculatorUseCases,
            exchangeUseCases: exchangeUseCases,
            favoriteUseCases: favoriteUseCases,
            currencySelectionUseCases: currencySelectionUseCases,
        )
        exchangeViewModel = ExchangeViewModel(
            exchangeUseCases: exchangeUseCases,
            favoriteUseCases: favoriteUseCases,
            currencySelectionUseCases: currencySelectionUseCases,
        )
        favoriteViewModel = FavoriteViewModel(
            exchangeUseCases: exchangeUseCases,
            favoriteUseCases: favoriteUseCases,
        )
        mainViewModel = MainViewModel(themeUseCases: themeUseCases)
    }
}
