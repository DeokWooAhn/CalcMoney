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
    var amountTestID: String = ""

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// 기본은 금액 옆에 통화 칩을 둔다. 접근성 글자 크기에서는 칩이 폭을 거의 다 차지해 금액 칸이 눌리므로 칩을 금액 아래로 내린다.
    private var amountRowLayout: AnyLayout {
        dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8))
            : AnyLayout(HStackLayout(spacing: 16))
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .scaledFont(size: 14)
                .foregroundStyle(.secondary)
                .padding(.leading, 4)

            amountRowLayout {
                if isEditable {
                    TextField(
                        "0",
                        text: Binding(get: { amount }, set: onAmountChange),
                    )
                    .scaledFont(size: 25, weight: .semibold)
                    .keyboardType(.decimalPad)
                    .accessibilityIdentifier(amountTestID)
                } else {
                    Text(amount.isEmpty ? "0" : amount)
                        .accessibilityIdentifier(amountTestID)
                        .scaledFont(size: 28, weight: .semibold)
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
                    // 금액이 길어져도 통화 칩이 눌려 코드가 잘리지 않게 칩은 제 크기를 지킨다.
                    .fixedSize()
                }
            }
            .padding(.horizontal, 15)
            .padding(.vertical, 10)
            .background(AppColors.surface)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        }
    }
}
