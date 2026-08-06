import Domain
import SwiftUI

/// 통화 선택 시트 (Android `CurrencyPickerDialog` 대응)
///
/// 정렬용 즐겨찾기 스냅샷과 아이콘용 실시간 즐겨찾기를 분리해,
/// 시트가 열려 있는 동안 하트를 눌러도 목록 순서가 튀지 않게 한다.
struct CurrencyPickerSheet: View {
    let currencies: [CurrencyInfo]
    let selectedCurrency: CurrencyInfo?
    let favoriteCurrencyCodesForSort: [String]
    let favoriteCurrencyCodesForIcon: [String]
    let title: String
    let onCurrencySelected: (CurrencyInfo) -> Void
    let onToggleFavorite: (String) -> Void

    @Environment(\.dismiss) private var dismiss

    private var sortedCurrencies: [CurrencyInfo] {
        let orderByCode = Dictionary(
            favoriteCurrencyCodesForSort.enumerated().map { ($0.element, $0.offset) },
            uniquingKeysWith: { first, _ in first },
        )
        let favorites = currencies
            .filter { orderByCode[$0.code] != nil }
            .sorted { (orderByCode[$0.code] ?? .max) < (orderByCode[$1.code] ?? .max) }
        let others = currencies.filter { orderByCode[$0.code] == nil }

        return favorites + others
    }

    var body: some View {
        NavigationStack {
            List(sortedCurrencies, id: \.code) { currency in
                CurrencyPickerRow(
                    currency: currency,
                    isSelected: currency == selectedCurrency,
                    isFavorite: favoriteCurrencyCodesForIcon.contains(currency.code),
                    onSelect: {
                        onCurrencySelected(currency)
                        dismiss()
                    },
                    onToggleFavorite: { onToggleFavorite(currency.code) },
                )
            }
            .listStyle(.plain)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .accessibilityLabel(L("닫기"))
                }
            }
        }
    }
}

private struct CurrencyPickerRow: View {
    let currency: CurrencyInfo
    let isSelected: Bool
    let isFavorite: Bool
    let onSelect: () -> Void
    let onToggleFavorite: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(currency.flagEmoji)
                .font(.system(size: 24))

            VStack(alignment: .leading, spacing: 2) {
                Text(currency.displayCode)
                    .font(.system(size: 16, weight: .semibold))

                Text(currency.localizedName)
                    .font(.system(size: 14))
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button(action: onToggleFavorite) {
                Image(systemName: isFavorite ? "heart.fill" : "heart")
                    .font(.system(size: 18))
                    .foregroundStyle(isFavorite ? .red : .secondary)
                    // .plain 스타일은 여백을 더하지 않아 아이콘 크기가 그대로 탭 영역이 된다.
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(isFavorite ? L("즐겨찾기 해제") : L("즐겨찾기 추가"))
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onSelect)
        .listRowBackground(isSelected ? AppColors.surface : Color.clear)
    }
}
