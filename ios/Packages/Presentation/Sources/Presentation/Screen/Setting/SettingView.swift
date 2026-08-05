import Domain
import SwiftUI

private let privacyPolicyURL = URL(string: "https://deokwooahn.github.io/CalcMoney/privacy-policy.html")!

/// 설정 화면 (Android `SettingScreen` 대응)
///
/// 환율 기준 정보는 공유 ExchangeViewModel의 상태를 읽는다.
struct SettingView: View {
    let exchangeViewModel: ExchangeViewModel
    let mainViewModel: MainViewModel
    let canRequestAds: Bool
    let isPrivacyOptionsRequired: Bool
    let onPrivacyOptionsTap: () -> Void

    @Environment(\.openURL) private var openURL
    @State private var isThemeExpanded = false
    @State private var isRateInfoExpanded = false
    @State private var snackbar = SnackbarPresenter()

    var body: some View {
        let exchangeState = exchangeViewModel.state
        let rateDateText = exchangeState.exchangeRateDate.isEmpty
            ? nil
            : ExchangeRateFormatters.formatRateDate(exchangeState.exchangeRateDate)
        let fetchedAtText = ExchangeRateFormatters.formatFetchedAt(exchangeState.exchangeRateFetchedAt)

        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                SettingSection(title: L("화면")) {
                    ThemeSettingCard(
                        selectedThemeMode: mainViewModel.themeMode,
                        isExpanded: $isThemeExpanded,
                        onThemeModeSelected: { mainViewModel.saveThemeMode($0) },
                    )
                }

                SettingSection(title: L("환율")) {
                    ExchangeRateInfoCard(
                        rateDate: rateDateText ?? L("정보 없음"),
                        lastUpdated: fetchedAtText ?? (exchangeState.isLoading ? L("불러오는 중") : L("정보 없음")),
                        isExpanded: $isRateInfoExpanded,
                    )
                }

                SettingSection(title: L("개인정보")) {
                    SettingActionCard(title: L("개인정보 처리방침")) {
                        openURL(privacyPolicyURL)
                    }

                    if isPrivacyOptionsRequired {
                        SettingActionCard(
                            title: L("개인정보 및 광고 설정"),
                            summary: L("광고 동의와 개인정보 선택 항목을 관리합니다."),
                            action: onPrivacyOptionsTap,
                        )
                    }
                }

                SettingSection(title: L("앱 정보")) {
                    SettingInfoCard(title: L("버전"), value: appVersion)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(AppColors.background)
        .overlay(alignment: .bottom) {
            if let message = snackbar.message {
                SnackbarView(message: message)
                    .padding(.bottom, 8)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .task {
            for await sideEffect in exchangeViewModel.sideEffects() {
                switch sideEffect {
                case .showSnackbar(let message):
                    snackbar.show(message)
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            AdMobBanner(adUnitID: AdConfig.settingsBannerUnitID, canRequestAds: canRequestAds)
        }
        .navigationTitle(L("설정"))
        .navigationBarTitleDisplayMode(.inline)
    }

    private var appVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "-"
    }
}

private struct SettingSection<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content

    var body: some View {
        Text(title)
            .font(.system(size: 13, weight: .semibold))
            .foregroundStyle(Color.secondary)
            .padding(.leading, 4)
            .padding(.top, 18)
            .padding(.bottom, 8)

        content
    }
}

/// 접을 수 있는 설정 카드 공통 틀 (Android `SettingCard` 대응)
private struct ExpandableSettingCard<Content: View>: View {
    let title: String
    let summary: String?
    @Binding var isExpanded: Bool
    @ViewBuilder let expandedContent: Content

    var body: some View {
        VStack(spacing: 0) {
            Button {
                withAnimation(.easeInOut(duration: 0.2)) {
                    isExpanded.toggle()
                }
            } label: {
                HStack {
                    Text(title)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(Color.primary)

                    Spacer()

                    if let summary {
                        Text(summary)
                            .font(.system(size: 13))
                            .foregroundStyle(Color.secondary)
                    }

                    Image(systemName: "chevron.down")
                        .font(.system(size: 13, weight: .semibold))
                        .foregroundStyle(Color.secondary)
                        .rotationEffect(.degrees(isExpanded ? 180 : 0))
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 14)
            }

            if isExpanded {
                VStack(alignment: .leading, spacing: 0) {
                    expandedContent
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 14)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(.bottom, 8)
    }
}

private struct ThemeSettingCard: View {
    let selectedThemeMode: ThemeMode
    @Binding var isExpanded: Bool
    let onThemeModeSelected: (ThemeMode) -> Void

    var body: some View {
        ExpandableSettingCard(
            title: L("테마"),
            summary: selectedThemeMode.label,
            isExpanded: $isExpanded,
        ) {
            ForEach(ThemeMode.allCases, id: \.self) { mode in
                Button {
                    onThemeModeSelected(mode)
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: mode == selectedThemeMode ? "largecircle.fill.circle" : "circle")
                            .font(.system(size: 18))
                            .foregroundStyle(mode == selectedThemeMode ? Color.accentColor : .secondary)

                        Text(mode.label)
                            .font(.system(size: 15))
                            .foregroundStyle(Color.primary)

                        Spacer()
                    }
                    .padding(.vertical, 8)
                    .contentShape(Rectangle())
                }
            }
        }
    }
}

private struct ExchangeRateInfoCard: View {
    let rateDate: String
    let lastUpdated: String
    @Binding var isExpanded: Bool

    var body: some View {
        ExpandableSettingCard(
            title: L("환율 기준 정보"),
            summary: rateDate,
            isExpanded: $isExpanded,
        ) {
            VStack(alignment: .leading, spacing: 10) {
                LabeledInfoRow(label: L("기준일"), value: rateDate)
                LabeledInfoRow(label: L("데이터 출처"), value: L("한국수출입은행 Open API"))
                LabeledInfoRow(label: L("마지막 갱신"), value: lastUpdated)

                Text(L("환율 정보는 영업일 11시 전후 고시되는 기준 환율을 사용합니다.\n주말·공휴일에는 새 환율이 제공되지 않을 수 있으며, 이 경우 직전 영업일 환율을 사용합니다."))
                    .font(.system(size: 12))
                    .foregroundStyle(Color.secondary)
                    .padding(.top, 4)

                Text(L("제공되는 환율은 참고용이며 실제 거래 환율과 다를 수 있습니다."))
                    .font(.system(size: 12))
                    .foregroundStyle(Color.secondary)
            }
        }
    }
}

private struct LabeledInfoRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top) {
            Text(label)
                .font(.system(size: 13))
                .foregroundStyle(Color.secondary)
                .frame(width: 80, alignment: .leading)

            Text(value)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(Color.primary)
        }
    }
}

private struct SettingActionCard: View {
    let title: String
    var summary: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 15, weight: .medium))
                        .foregroundStyle(Color.primary)

                    if let summary {
                        Text(summary)
                            .font(.system(size: 12))
                            .foregroundStyle(Color.secondary)
                    }
                }

                Spacer()

                Image(systemName: "arrow.up.forward.square")
                    .font(.system(size: 16))
                    .foregroundStyle(Color.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(AppColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .padding(.bottom, 8)
    }
}

private struct SettingInfoCard: View {
    let title: String
    let value: String

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 15, weight: .medium))

            Spacer()

            Text(value)
                .font(.system(size: 13))
                .foregroundStyle(Color.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(.bottom, 8)
    }
}

private extension ThemeMode {
    var label: String {
        switch self {
        case .system: L("시스템")
        case .light: L("라이트")
        case .dark: L("다크")
        }
    }
}
