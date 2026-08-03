import SwiftUI

/// 앱 루트 화면. 탭 전환은 TabView, 탭 내부 계층 이동은 각 탭의 NavigationStack이 담당한다.
public struct MainTabView: View {
    @State private var selectedTab: MainTab = .calculator

    public init() {}

    public var body: some View {
        TabView(selection: $selectedTab) {
            ForEach(MainTab.allCases) { tab in
                NavigationStack {
                    rootScreen(for: tab)
                        .navigationTitle(tab.title)
                }
                .tabItem {
                    Label(tab.title, systemImage: tab.systemImage)
                }
                .tag(tab)
            }
        }
    }

    @ViewBuilder
    private func rootScreen(for tab: MainTab) -> some View {
        switch tab {
        case .calculator: CalculatorView()
        case .exchange: ExchangeView()
        case .favorite: FavoriteView()
        case .setting: SettingView()
        }
    }
}

#Preview {
    MainTabView()
}
