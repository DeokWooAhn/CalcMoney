import SwiftUI

/// 앱 루트 화면. 탭 전환은 TabView, 탭 내부 계층 이동은 각 탭의 NavigationStack이 담당한다.
///
/// ExchangeViewModel은 Android의 activity-scoped 공유처럼 앱에서 하나만 만들어
/// Exchange·Favorite·Setting 탭이 함께 쓴다.
public struct MainTabView: View {
    @State private var selectedTab: MainTab = .calculator

    private let calculatorViewModel: CalculatorViewModel
    private let exchangeViewModel: ExchangeViewModel
    private let favoriteViewModel: FavoriteViewModel
    private let mainViewModel: MainViewModel
    private let adConsentManager: AdConsentManager

    public init(
        calculatorViewModel: CalculatorViewModel,
        exchangeViewModel: ExchangeViewModel,
        favoriteViewModel: FavoriteViewModel,
        mainViewModel: MainViewModel,
        adConsentManager: AdConsentManager,
    ) {
        self.calculatorViewModel = calculatorViewModel
        self.exchangeViewModel = exchangeViewModel
        self.favoriteViewModel = favoriteViewModel
        self.mainViewModel = mainViewModel
        self.adConsentManager = adConsentManager
    }

    public var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(MainTab.allCases) { tab in
                NavigationStack {
                    rootScreen(for: tab)
                }
                .tabItem {
                    Label(tab.title, systemImage: tab.systemImage)
                }
                .tag(tab)
            }
        }
        .preferredColorScheme(mainViewModel.themeMode.colorScheme)
        .task {
            // Google 정책: 앱 시작 시마다 동의 상태를 갱신한다.
            await adConsentManager.gatherConsent()
        }
    }

    @ViewBuilder
    private func rootScreen(for tab: MainTab) -> some View {
        switch tab {
        case .calculator:
            CalculatorView(viewModel: calculatorViewModel)

        case .exchange:
            ExchangeView(
                viewModel: exchangeViewModel,
                canRequestAds: adConsentManager.canRequestAds,
            )

        case .favorite:
            FavoriteView(
                exchangeViewModel: exchangeViewModel,
                favoriteViewModel: favoriteViewModel,
                canRequestAds: adConsentManager.canRequestAds,
            )

        case .setting:
            SettingView(
                exchangeViewModel: exchangeViewModel,
                mainViewModel: mainViewModel,
                canRequestAds: adConsentManager.canRequestAds,
                isPrivacyOptionsRequired: adConsentManager.isPrivacyOptionsRequired,
                onPrivacyOptionsTap: {
                    Task { await adConsentManager.presentPrivacyOptionsForm() }
                },
            )
        }
    }
}
