import Domain
import SwiftUI

/// 현재 통화를 보여주고 탭하면 통화 선택 시트를 여는 캡슐 버튼 (Android `CurrencySelector` 대응)
struct CurrencySelectorView: View {
    let selectedCurrency: CurrencyInfo?
    let availableCurrencies: [CurrencyInfo]
    let favoriteCurrencyCodes: [String]
    var dialogTitle = L("통화 선택")
    var compact = false
    let onCurrencySelected: (CurrencyInfo) -> Void
    let onToggleFavorite: (String) -> Void

    @State private var showPicker = false
    @State private var favoriteCodesSnapshot: [String] = []

    var body: some View {
        Button {
            favoriteCodesSnapshot = favoriteCurrencyCodes
            showPicker = true
        } label: {
            HStack(spacing: compact ? 4 : 8) {
                if let selectedCurrency {
                    Text(selectedCurrency.flagEmoji)
                        .font(.system(size: compact ? 15 : 20))

                    Text(selectedCurrency.code)
                        .font(.system(size: compact ? 14 : 16, weight: .semibold))
                        .foregroundStyle(Color.primary)

                    if !compact {
                        Image(systemName: "chevron.down")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundStyle(Color.primary)
                    }
                } else {
                    Text(L("통화 선택"))
                        .font(.system(size: compact ? 14 : 16))
                        .foregroundStyle(Color.primary)
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            // 내용 높이가 20pt 안팎이라 패딩만으로는 최소 터치 영역에 못 미친다.
            .frame(minHeight: 44)
            .background(AppColors.currencySelectorSurface)
            .clipShape(RoundedRectangle(cornerRadius: compact ? 14 : 12))
            .overlay(
                RoundedRectangle(cornerRadius: compact ? 14 : 12)
                    .strokeBorder(AppColors.currencySelectorBorder, lineWidth: 1),
            )
        }
        .disabled(availableCurrencies.isEmpty)
        .sheet(isPresented: $showPicker) {
            CurrencyPickerSheet(
                currencies: availableCurrencies,
                selectedCurrency: selectedCurrency,
                favoriteCurrencyCodesForSort: favoriteCodesSnapshot,
                favoriteCurrencyCodesForIcon: favoriteCurrencyCodes,
                title: dialogTitle,
                onCurrencySelected: onCurrencySelected,
                onToggleFavorite: onToggleFavorite,
            )
        }
    }
}
