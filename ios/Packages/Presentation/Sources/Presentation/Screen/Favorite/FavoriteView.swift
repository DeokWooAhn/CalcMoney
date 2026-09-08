import Domain
import SwiftUI

/// 즐겨찾기 화면 (Android `FavoriteScreen` 대응)
///
/// 기준 금액·통화는 공유 ExchangeViewModel의 상태를 그대로 쓴다.
struct FavoriteView: View {
    let exchangeViewModel: ExchangeViewModel
    let favoriteViewModel: FavoriteViewModel
    let canRequestAds: Bool

    @State private var snackbar = SnackbarPresenter()

    var body: some View {
        let exchangeState = exchangeViewModel.state
        let favoriteState = favoriteViewModel.state

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
                favoriteState: favoriteState,
                onRemoveFavorite: { exchangeViewModel.send(.toggleFavorite($0)) },
            )
        }
        .padding(.horizontal, 10)
        .padding(.top, 8)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
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
        .onChange(of: FavoriteRateInputSnapshot(exchangeState), initial: true) { _, snapshot in
            favoriteViewModel.onExchangeStateChanged(
                fromCurrency: snapshot.fromCurrency,
                favoriteCurrencyCodes: snapshot.favoriteCurrencyCodes,
                availableCurrencies: snapshot.availableCurrencies,
                exchangeRateDate: snapshot.exchangeRateDate,
                exchangeRateFetchedAt: snapshot.exchangeRateFetchedAt,
            )
        }
        .onChange(of: exchangeState.fromAmount, initial: true) { _, fromAmount in
            favoriteViewModel.onBaseAmountChanged(fromAmount)
        }
        .safeAreaInset(edge: .bottom) {
            AdMobBanner(adUnitID: AdConfig.favoriteBannerUnitID, canRequestAds: canRequestAds)
        }
        .navigationTitle(L("즐겨찾기"))
        .navigationBarTitleDisplayMode(.inline)
        .scrollDismissesKeyboard(.interactively)
    }
}

/// Android `LaunchedEffect` 키 대응 — 환율 상태 중 즐겨찾기 계산에 필요한 부분만 비교한다.
private struct FavoriteRateInputSnapshot: Equatable {
    let fromCurrency: CurrencyInfo?
    let favoriteCurrencyCodes: [String]
    let availableCurrencies: [CurrencyInfo]
    let exchangeRateDate: String
    let exchangeRateFetchedAt: Int

    init(_ state: ExchangeState) {
        fromCurrency = state.fromCurrency
        favoriteCurrencyCodes = state.favoriteCurrencyCodes
        availableCurrencies = state.availableCurrencies
        exchangeRateDate = state.exchangeRateDate
        exchangeRateFetchedAt = state.exchangeRateFetchedAt
    }
}

private struct FavoriteRateContent: View {
    let exchangeState: ExchangeState
    let favoriteState: FavoriteState
    let onRemoveFavorite: (String) -> Void

    var body: some View {
        if !favoriteState.items.isEmpty {
            FavoriteRateGrid(
                items: favoriteState.items,
                isRefreshing: favoriteState.isLoading,
                onRemoveFavorite: onRemoveFavorite,
            )
        } else if favoriteState.isLoading {
            CenteredMessage { ProgressView() }
        } else if exchangeState.favoriteCurrencyCodes.isEmpty {
            CenteredMessage { Text(L("즐겨찾기한 통화가 없습니다.")).foregroundStyle(.secondary) }
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
    let items: [FavoriteState.Item]
    let isRefreshing: Bool
    let onRemoveFavorite: (String) -> Void

    private let columns = [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)]

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
    let item: FavoriteState.Item
    let onRemoveFavorite: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 8) {
                Text(item.currency.flagEmoji)
                    .font(.system(size: 22))
                    // 바로 옆 통화 코드·이름과 같은 정보라 스크린 리더가 이모지 이름까지 읽을 필요가 없다.
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 0) {
                    Text(item.currency.code)
                        .font(.system(size: 15, weight: .semibold))

                    Text(item.currency.localizedName)
                        .font(.system(size: 12))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }

                Spacer()

                Button(action: onRemoveFavorite) {
                    Image(systemName: "heart.fill")
                        .font(.system(size: 18))
                        .foregroundStyle(.red)
                        // Android 대응 구현은 requiredSize(48.dp). iOS HIG 최소치는 44pt다.
                        .frame(width: 44, height: 44)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(L("즐겨찾기 해제"))
            }

            Text(item.convertedAmount)
                .font(.system(size: 20, weight: .bold))
                .lineLimit(1)
                .minimumScaleFactor(0.6)

            Text(item.rateLabel)
                .font(.system(size: 11))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(AppColors.surface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}
