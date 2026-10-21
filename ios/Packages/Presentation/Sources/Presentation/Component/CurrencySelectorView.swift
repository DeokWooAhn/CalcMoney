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

    /// 시트를 여는 순간의 즐겨찾기. 시트가 떠 있는 동안 하트를 눌러도 목록 순서가 튀지 않게 정렬에만 쓴다.
    ///
    /// `.sheet(isPresented:)`의 내용 클로저에서 따로 둔 `@State`를 읽으면, body가 그 값에 의존하지 않아
    /// 시트가 탭하기 전 값(빈 배열)을 받는다. 스냅숏을 시트의 item으로 넘겨 탭한 순간의 값을 그대로 쓴다.
    @State private var pickerSnapshot: FavoriteCodesSnapshot?

    var body: some View {
        Button {
            pickerSnapshot = FavoriteCodesSnapshot(codes: favoriteCurrencyCodes)
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
        .accessibilityIdentifier("currency.selector")
        .disabled(availableCurrencies.isEmpty)
        .sheet(item: $pickerSnapshot) { snapshot in
            CurrencyPickerSheet(
                currencies: availableCurrencies,
                selectedCurrency: selectedCurrency,
                favoriteCurrencyCodesForSort: snapshot.codes,
                favoriteCurrencyCodesForIcon: favoriteCurrencyCodes,
                title: dialogTitle,
                onCurrencySelected: onCurrencySelected,
                onToggleFavorite: onToggleFavorite,
            )
        }
    }
}

private struct FavoriteCodesSnapshot: Identifiable {
    let id = UUID()
    let codes: [String]
}
