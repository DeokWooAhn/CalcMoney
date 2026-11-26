import Domain
import SwiftUI

/// 즐겨찾기 화면 (Android `FavoriteScreen` 대응)
///
/// 기준 금액·통화와 즐겨찾기 카드 모두 공유 ExchangeViewModel의 상태를 그대로 쓴다.
/// 카드를 언제 다시 모을지는 ExchangeViewModel이 정하므로, 이 화면은 그리고 Intent를 보내기만 한다.
struct FavoriteView: View {
    let exchangeViewModel: ExchangeViewModel
    let canRequestAds: Bool

    @State private var snackbar = SnackbarPresenter()

    var body: some View {
        let exchangeState = exchangeViewModel.state

        VStack(spacing: 16) {
            ExchangeInputContainer(
                label: L("기준 금액"),
                amount: exchangeState.fromAmount,
                currency: exchangeState.fromCurrency,
                availableCurrencies: exchangeState.availableCurrencies,
                favoriteCurrencyCodes: exchangeState.favoriteCurrencyCodes,
                isEditable: true,
                onAmountChange: { exchangeViewModel.send(.updateFromAmount($0)) },
                onCurrencySelected: { exchangeViewModel.send(.selectFromCurrency($0)) },
                onToggleFavorite: { exchangeViewModel.send(.toggleFavorite($0)) },
            )

            FavoriteRateContent(
                exchangeState: exchangeState,
                onRemoveFavorite: { exchangeViewModel.send(.toggleFavorite($0)) },
                onRetry: { exchangeViewModel.send(.refreshExchangeRates) },
            )
        }
        .padding(.horizontal, 10)
        .padding(.top, 8)
        .readableContentWidth()
        .frame(maxHeight: .infinity, alignment: .top)
        .background(AppColors.background)
        .snackbarOverlay(snackbar)
        .task {
            for await sideEffect in exchangeViewModel.sideEffects() {
                switch sideEffect {
                case let .showSnackbar(message):
                    snackbar.show(message)
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            AdMobBanner(adUnitID: AdConfig.favoriteBannerUnitID, canRequestAds: canRequestAds)
        }
        .navigationTitle(L("즐겨찾기"))
        .navigationBarTitleDisplayMode(.inline)
        .scrollDismissesKeyboard(.interactively)
    }
}

private struct FavoriteRateContent: View {
    let exchangeState: ExchangeState
    let onRemoveFavorite: (String) -> Void
    let onRetry: () -> Void

    var body: some View {
        let favoriteRates = exchangeState.favoriteRates

        if !favoriteRates.items.isEmpty {
            FavoriteRateGrid(
                items: favoriteRates.items,
                isRefreshing: favoriteRates.isLoading,
                onRemoveFavorite: onRemoveFavorite,
            )
        } else if favoriteRates.isLoading {
            CenteredMessage { ProgressView() }
        } else if exchangeState.favoriteCurrencyCodes.isEmpty {
            CenteredMessage { Text(L("즐겨찾기한 통화가 없습니다.")).foregroundStyle(.secondary) }
        } else if let baseCode = favoriteRates.baseOnlyFavoriteCode {
            CenteredMessage {
                Text(L("현재 기준 통화가 \(baseCode)입니다.\n다른 통화를 즐겨찾기에 추가해 주세요."))
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        } else if let failure = exchangeState.failure {
            // 새로고침은 성공하기 전까지 `isLoading` 말고는 상태를 바꾸지 않는다.
            // 그래서 `FavoriteRateInput`이 그대로라 위의 `favoriteRates.isLoading` 분기가 끼어들지 않고,
            // 진행 표시가 이 분기에서 끝까지 보인다.
            CenteredMessage {
                LoadFailureMessage(message: failure.message, isRetrying: exchangeState.isLoading, onRetry: onRetry)
                    .accessibilityIdentifier("favorite.failure")
            }
        } else {
            CenteredMessage { Text(L("환율 정보를 불러올 수 없습니다.")).foregroundStyle(.secondary) }
        }
    }
}

private struct CenteredMessage<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        VStack {
            Spacer()
            content
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

private struct FavoriteRateGrid: View {
    let items: [FavoriteRateItem]
    let isRefreshing: Bool
    let onRemoveFavorite: (String) -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// 기본은 2열이다. 글자를 기본보다 키우면 1열로 둔다.
    /// 반 폭 카드는 국기와 하트 버튼(44pt) 사이 이름 칸이 iPhone SE에서 45pt 안팎이라, 한 단계(xLarge)만 키워도
    /// "호주 달러"가 한 줄에 들지 않고, 두 줄로 늘려도 "아랍에미리트 디르함"은 잘린다.
    private var columns: [GridItem] {
        let columnCount = dynamicTypeSize >= .xLarge ? 1 : 2

        return Array(repeating: GridItem(.flexible(), spacing: 8), count: columnCount)
    }

    var body: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(items, id: \.currency.code) { item in
                    FavoriteRateCard(
                        item: item,
                        onRemoveFavorite: { onRemoveFavorite(item.currency.code) },
                    )
                }
            }
            .padding(.bottom, 24)
        }
        .overlay(alignment: .topTrailing) {
            if isRefreshing {
                ProgressView()
                    .padding(12)
                    .accessibilityLabel(L("환율 정보를 새로고침하는 중입니다."))
            }
        }
    }
}

private struct FavoriteRateCard: View {
    let item: FavoriteRateItem
    let onRemoveFavorite: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(item.currency.flagEmoji)
                    .scaledFont(size: 22)
                    // 바로 옆 통화 코드·이름과 같은 정보라 스크린 리더가 이모지 이름까지 읽을 필요가 없다.
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 0) {
                    Text(item.currency.code)
                        .scaledFont(size: 15, weight: .semibold)

                    Text(item.currency.localizedName)
                        .scaledFont(size: 12)
                        .foregroundStyle(.secondary)
                        // 짧은 이름은 한 줄 그대로이고, 칸보다 긴 이름은 "…"로 자르지 않고 두 줄까지 보인다.
                        .lineLimit(2)
                }

                Spacer()

                Button(action: onRemoveFavorite) {
                    Image(systemName: "heart.fill")
                        .scaledFont(size: 18)
                        .foregroundStyle(.red)
                        // Android 대응 구현은 requiredSize(48.dp). iOS HIG 최소치는 44pt다.
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(L("즐겨찾기 해제"))
            }

            Text(item.convertedAmount)
                .scaledFont(size: 20, weight: .bold)
                .lineLimit(1)
                .minimumScaleFactor(0.6)

            Text(item.rateLabel)
                .scaledFont(size: 11)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .accessibilityElement(children: .contain)
        .accessibilityIdentifier("favorite.card.\(item.currency.code)")
    }
}
