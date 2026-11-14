import Data
import Domain
import Foundation
import OSLog
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
            reviewPromptUseCases: useCases.reviewPrompt,
            interstitialAdUseCases: useCases.interstitialAd,
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
        mainViewModel = MainViewModel(
            themeUseCases: useCases.theme,
            reviewPromptUseCases: useCases.reviewPrompt,
            interstitialAdUseCases: useCases.interstitialAd,
            crashBreadcrumbs: repositories.crashBreadcrumbs,
        )
    }
}

/// Data 레이어 구현체 모음 (Android `RepositoryModule` 대응)
private struct Repositories {
    let exchangeRate: any ExchangeRateRepository
    let currencySelection: any CurrencySelectionRepository
    let favoriteCurrency: any FavoriteCurrencyRepository
    let theme: any ThemeRepository
    let calculatorHistory: any CalculatorHistoryRepository
    let reviewPrompt: any ReviewPromptRepository
    let interstitialAd: any InterstitialAdRepository
    /// 크래시 단서. Repository는 아니지만 Firebase 초기화 여부에 따라 구현이 갈려 여기서 함께 만든다.
    let crashBreadcrumbs: any CrashBreadcrumbs

    init() {
        // GoogleService-Info.plist가 없으면 원격 환율은 "준비 안 됨" 상태로 동작한다.
        let isFirebaseConfigured = !Self.usesFixedRates && FirebaseBootstrap.configureIfAvailable()
        let remoteDataSource = Self.makeRemoteDataSource(isFirebaseConfigured: isFirebaseConfigured)
        // Crashlytics는 Firebase를 초기화하기 전에 부르면 크래시하므로 초기화했을 때만 쓴다.
        crashBreadcrumbs = isFirebaseConfigured ? FirebaseCrashBreadcrumbs() : NoOpCrashBreadcrumbs()

        exchangeRate = ExchangeRateRepositoryImpl(
            remoteDataSource: remoteDataSource,
            localDataSource: Self.makeExchangeRateLocalDataSource(),
            crashBreadcrumbs: crashBreadcrumbs,
        )
        currencySelection = CurrencySelectionRepositoryImpl(dataSource: CurrencySelectionDataSource())
        favoriteCurrency = FavoriteCurrencyRepositoryImpl(dataSource: FavoriteCurrencyDataSource())
        theme = ThemeRepositoryImpl(dataSource: ThemePreferenceDataSource())
        calculatorHistory = CalculatorHistoryRepositoryImpl(dataSource: CalculatorHistoryDataSource())
        reviewPrompt = ReviewPromptRepositoryImpl(dataSource: ReviewPromptDataSource())
        interstitialAd = InterstitialAdRepositoryImpl(dataSource: InterstitialAdDataSource())
    }

    /// 디스크 캐시를 만들지 못하면 메모리 캐시로 내려간다.
    ///
    /// 저장소 손상·용량 부족·마이그레이션 실패로 앱 전체를 못 쓰게 만드는 것보다,
    /// 이번 실행 동안만 캐시가 유지되는 편이 낫다. 계산기 탭은 캐시와 무관하게 동작하고
    /// 환율도 원격에서 다시 받아 오므로 기능은 그대로 살아 있다.
    private static func makeExchangeRateLocalDataSource() -> ExchangeRateLocalDataSource {
        let logger = Logger(subsystem: "com.deokwooahn.calcmoney", category: "AppContainer")

        do {
            return try ExchangeRateLocalDataSource.make(inMemory: usesFixedRates)
        } catch {
            logger.error("Falling back to in-memory exchange rate cache: \(error.localizedDescription)")
        }

        do {
            return try ExchangeRateLocalDataSource.make(inMemory: true)
        } catch {
            // 메모리 저장소마저 실패하면 SwiftData 스키마 자체가 깨졌다는 뜻이라 복구할 방법이 없다.
            fatalError("Failed to create the exchange rate cache store: \(error)")
        }
    }

    private static var usesFixedRates: Bool {
        #if DEBUG
            let arguments = ProcessInfo.processInfo.arguments
            return arguments.contains("UI_TESTING") && arguments.contains("FIXED_EXCHANGE_RATES")
        #else
            return false
        #endif
    }

    private static func makeRemoteDataSource(isFirebaseConfigured: Bool) -> any ExchangeRateRemoteDataSource {
        #if DEBUG
            if usesFixedRates { return FixedExchangeRateDataSource() }
        #endif
        return isFirebaseConfigured ? FirestoreExchangeRateDataSource() : UnavailableExchangeRateRemoteDataSource()
    }
}

#if DEBUG
    /// 환율 원본만 고정하고 실제 Repository·UseCase·화면의 연결을 검증한다.
    private struct FixedExchangeRateDataSource: ExchangeRateRemoteDataSource {
        func fetchExchangeRates() async throws -> [ExchangeRateData] {
            let fetchedAt = Int(Date().timeIntervalSince1970 * 1000)
            return [
                ExchangeRateData(
                    code: "USD",
                    currencyUnit: "USD",
                    currencyName: "미국 달러",
                    baseRate: 1000,
                    fetchedAt: fetchedAt,
                    rateDate: "2026-10-15",
                ),
                ExchangeRateData(
                    code: "AUD",
                    currencyUnit: "AUD",
                    currencyName: "호주 달러",
                    baseRate: 500,
                    fetchedAt: fetchedAt,
                    rateDate: "2026-10-15",
                ),
            ]
        }
    }
#endif

/// Domain 유스케이스 묶음 모음 (Android의 UseCases 그룹 대응)
private struct UseCases {
    let calculator: CalculatorUseCases
    let exchange: ExchangeUseCases
    let favorite: FavoriteUseCases
    let currencySelection: CurrencySelectionUseCases
    let theme: ThemeUseCases
    let reviewPrompt: ReviewPromptUseCases
    let interstitialAd: InterstitialAdUseCases

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
        reviewPrompt = ReviewPromptUseCases(
            recordAppOpen: RecordAppOpenUseCase(repository: repositories.reviewPrompt),
            recordCalculationSuccess: RecordCalculationSuccessUseCase(repository: repositories.reviewPrompt),
        )
        interstitialAd = InterstitialAdUseCases(
            recordCalculation: RecordInterstitialAdCalculationUseCase(repository: repositories.interstitialAd),
            shouldShow: ShouldShowInterstitialAdUseCase(repository: repositories.interstitialAd),
            recordShown: RecordInterstitialAdShownUseCase(repository: repositories.interstitialAd),
        )
    }
}
