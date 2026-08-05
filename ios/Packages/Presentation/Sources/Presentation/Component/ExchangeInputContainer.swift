import Domain
import SwiftUI

/// 금액 입력 + 통화 선택이 붙은 카드 (Android `ExchangeInputContainer` 대응)
struct ExchangeInputContainer: View {
    let label: String
    let amount: String
    let currency: CurrencyInfo?
    let availableCurrencies: [CurrencyInfo]
    let favoriteCurrencyCodes: [String]
    let isEditable: Bool
    let onAmountChange: (String) -> Void
    let onCurrencySelected: (CurrencyInfo) -> Void
    let onToggleFavorite: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 14))
                .foregroundStyle(.secondary)
                .padding(.leading, 4)

            HStack(spacing: 16) {
                if isEditable {
                    TextField(
                        "0",
                        text: Binding(get: { amount }, set: onAmountChange),
                    )
                    .font(.system(size: 25, weight: .semibold))
                    .keyboardType(.decimalPad)
                } else {
                    Text(amount.isEmpty ? "0" : amount)
                        .font(.system(size: 28, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.5)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if currency != nil {
                    CurrencySelectorView(
                        selectedCurrency: currency,
                        availableCurrencies: availableCurrencies,
                        favoriteCurrencyCodes: favoriteCurrencyCodes,
                        onCurrencySelected: onCurrencySelected,
                        onToggleFavorite: onToggleFavorite,
                    )
                }
            }
            .padding(.horizontal, 15)
            .padding(.vertical, 10)
            .background(AppColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}
