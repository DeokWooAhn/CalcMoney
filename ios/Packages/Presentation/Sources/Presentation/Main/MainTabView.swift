import SwiftUI

/// 앱 루트 화면. 탭 전환은 TabView, 탭 내부 계층 이동은 각 탭의 NavigationStack이 담당한다.
///
/// ExchangeViewModel은 Android의 activity-scoped 공유처럼 앱에서 하나만 만들어
/// Exchange·Favorite·Setting 탭이 함께 쓴다.
public struct MainTabView: View {
    @State private var selectedTab: MainTab = .calculator
    @State private var interstitialAd = InterstitialAdPresenter(adUnitID: AdConfig.calculatorInterstitialUnitID)
    @Environment(\.scenePhase) private var scenePhase

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
                        // 식별자만 달면 id가 없는 자식 요소(글자 등)가 모두 같은 id를 물려받아
                        // 기준점 하나가 여러 요소와 매칭된다. .contain으로 컨테이너 요소를 하나 따로 만든다.
                        .accessibilityElement(children: .contain)
                        .accessibilityIdentifier(tab.screenTestID)
                }
                .tabItem {
                    Label(tab.title, systemImage: tab.systemImage)
                        .accessibilityIdentifier(tab.testID)
                }
                .tag(tab)
            }
        }
        .preferredColorScheme(mainViewModel.themeMode.colorScheme)
        .task {
            // Google 정책: 앱 시작 시마다 동의 상태를 갱신한다.
            await adConsentManager.gatherConsent()
        }
        // 처음 뜰 때와 백그라운드에서 돌아올 때 모두 센다 (Android `MainActivity.onStart` 대응).
        .onChange(of: scenePhase, initial: true) { _, phase in
            if phase == .active {
                mainViewModel.recordAppOpen()
            }
        }
        .onChange(of: adConsentManager.canRequestAds, initial: true) { _, canRequestAds in
            if canRequestAds {
                interstitialAd.preload()
            }
        }
        // 계산을 마치고 다른 탭으로 넘어가는 순간에만 전면 광고를 고려한다 (Android `MainScreen.onLeaveCalculator` 대응).
        .onChange(of: selectedTab) { oldTab, newTab in
            if oldTab == .calculator, newTab != .calculator {
                showInterstitialAdIfDue()
            }
        }
    }

    /// 광고를 보여 줄 차례이고 받아 둔 광고가 있으면 보여 준다. 탭은 이미 넘어간 뒤라 광고를 닫으면 고른 탭이 보인다.
    ///
    /// 광고 동의 전이나 UI 테스트에서는 canRequestAds가 false라 광고를 받지도 보여 주지도 않는다.
    private func showInterstitialAdIfDue() {
        guard adConsentManager.canRequestAds else { return }
        guard interstitialAd.isReady else {
            interstitialAd.preload()
            return
        }

        Task {
            guard await mainViewModel.shouldShowInterstitialAd() else { return }
            // 기록을 읽는 사이 계산기로 돌아왔거나 광고 동의를 철회했으면 띄우지 않는다.
            guard selectedTab != .calculator, adConsentManager.canRequestAds else { return }

            interstitialAd.present(onShown: mainViewModel.recordInterstitialAdShown)
        }
    }

    @ViewBuilder
    private func rootScreen(for tab: MainTab) -> some View {
        switch tab {
        case .calculator:
            CalculatorView(viewModel: calculatorViewModel, onReviewRequested: mainViewModel.markReviewRequested)

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
