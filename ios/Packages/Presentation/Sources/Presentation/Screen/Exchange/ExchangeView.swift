import Domain
import SwiftUI

/// 환율 화면 (Android `ExchangeScreen` 대응)
struct ExchangeView: View {
    let viewModel: ExchangeViewModel
    let canRequestAds: Bool

    @State private var snackbar = SnackbarPresenter()

    var body: some View {
        let state = viewModel.state

        ScrollView {
            VStack(spacing: 16) {
                ExchangeInputContainer(
                    label: L("기준 금액"),
                    amount: state.fromAmount,
                    currency: state.fromCurrency,
                    availableCurrencies: state.availableCurrencies,
                    favoriteCurrencyCodes: state.favoriteCurrencyCodes,
                    isEditable: true,
                    onAmountChange: { viewModel.send(.updateFromAmount($0)) },
                    onCurrencySelected: { viewModel.send(.selectFromCurrency($0)) },
                    onToggleFavorite: { viewModel.send(.toggleFavorite($0)) },
                )

                SwapCurrencyButton {
                    viewModel.send(.swapCurrencies)
                }

                ExchangeInputContainer(
                    label: L("받을 금액"),
                    amount: state.toAmount,
                    currency: state.toCurrency,
                    availableCurrencies: state.availableCurrencies,
                    favoriteCurrencyCodes: state.favoriteCurrencyCodes,
                    isEditable: false,
                    onAmountChange: { _ in },
                    onCurrencySelected: { viewModel.send(.selectToCurrency($0)) },
                    onToggleFavorite: { viewModel.send(.toggleFavorite($0)) },
                )

                ExchangeRateInfoView(state: state)
            }
            .padding(.horizontal, 10)
            .padding(.top, 8)
        }
        .background(AppColors.background)
        .refreshable {
            // send(_:)는 Task만 띄우고 반환해서 새로고침 표시가 즉시 사라진다.
            await viewModel.refreshExchangeRates()
        }
        .overlay(alignment: .topTrailing) {
            if viewModel.state.isLoading {
                ProgressView()
                    .padding(16)
            }
        }
        .snackbarOverlay(snackbar)
        .task {
            for await sideEffect in viewModel.sideEffects() {
                switch sideEffect {
                case let .showSnackbar(message):
                    snackbar.show(message)
                }
            }
        }
        .safeAreaInset(edge: .bottom) {
            AdMobBanner(adUnitID: AdConfig.exchangeBannerUnitID, canRequestAds: canRequestAds)
        }
        .navigationTitle(L("환율"))
        .navigationBarTitleDisplayMode(.inline)
        .scrollDismissesKeyboard(.interactively)
    }
}

private struct SwapCurrencyButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "arrow.up.arrow.down")
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(Color.primary)
                .frame(width: 56, height: 56)
                .background(AppColors.surface)
                .clipShape(Circle())
        }
        .accessibilityLabel(L("통화 교환"))
    }
}

private struct ExchangeRateInfoView: View {
    let state: ExchangeState

    var body: some View {
        if let fromCurrency = state.fromCurrency, let toCurrency = state.toCurrency, state.exchangeRate > 0 {
            VStack(spacing: 4) {
                Text("1 \(fromCurrency.code) = \(String(format: "%.4f", state.exchangeRate)) \(toCurrency.code)")

                if !state.exchangeRateDate.isEmpty {
                    Text(L("환율 기준: \(ExchangeRateFormatters.formatRateDate(state.exchangeRateDate))"))
                }
            }
            .font(.system(size: 12))
            .foregroundStyle(Color.secondary)
            .padding(.top, 8)
        }
    }
}
