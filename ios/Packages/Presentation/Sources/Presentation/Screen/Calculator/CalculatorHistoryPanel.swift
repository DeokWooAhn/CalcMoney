import SwiftUI

/// 계산 기록 패널 (Android `CalculatorHistoryPanel` 대응)
struct CalculatorHistoryPanel: View {
    let histories: [CalculatorState.HistoryItem]
    let colors: CalculatorColors
    let onClearHistory: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Text(L("계산 기록"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(colors.keyText)

                Spacer()

                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundStyle(colors.preview)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)

            if histories.isEmpty {
                Spacer()

                Text(L("계산 기록이 없습니다."))
                    .font(.system(size: 14))
                    .foregroundStyle(colors.preview)

                Spacer()
            } else {
                ScrollView {
                    LazyVStack(alignment: .trailing, spacing: 10) {
                        ForEach(Array(histories.reversed().enumerated()), id: \.offset) { _, item in
                            VStack(alignment: .trailing, spacing: 2) {
                                Text(CalculatorFormatting.formatWithCommas(item.expression))
                                    .font(.system(size: 14))
                                    .foregroundStyle(colors.preview)

                                Text("= \(CalculatorFormatting.formatWithCommas(item.result))")
                                    .font(.system(size: 17, weight: .medium))
                                    .foregroundStyle(colors.keyText)
                            }
                            .frame(maxWidth: .infinity, alignment: .trailing)
                        }
                    }
                    .padding(.horizontal, 16)
                }

                Button(action: onClearHistory) {
                    Text(L("계산 기록 삭제"))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(colors.destructive)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                }
            }
        }
        .background(colors.keyBackground)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.25), radius: 12, x: 0, y: 4)
    }
}
