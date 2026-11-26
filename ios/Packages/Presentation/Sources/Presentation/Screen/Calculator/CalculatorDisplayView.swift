import Domain
import SwiftUI

/// 수식·미리보기 표시 영역 (Android `CalculatorDisplay` 대응)
struct CalculatorDisplayView: View {
    let state: CalculatorState
    let colors: CalculatorColors

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    /// 수식 칸 맨 아래(환산 금액 아래)를 가리키는 스크롤 위치
    private static let expressionBottomID = "calculator.expression.bottom"
    /// 미리보기 결과의 기본 글자 크기에서의 크기
    static let previewFontSize: CGFloat = 30

    /// 디스플레이 글자가 기본 크기보다 커진 비율.
    ///
    /// 작은 글자일수록 더 많이 커지므로, 디스플레이에서 가장 작은 환산 금액 글자의 비율로 넉넉히 잡는다.
    static func textScale(for dynamicTypeSize: DynamicTypeSize) -> CGFloat {
        let baseSize = ConvertedAmountText.fontSize

        return ScaledFont.size(baseSize, for: dynamicTypeSize) / baseSize
    }

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            // 수식이 칸보다 길어지면 줄이 위로 밀려 올라가고, 지금 입력하는 끝부분이 늘 보이게 맨 아래로 스크롤한다.
            // 스크롤 없이 두면 높이가 모자란 화면(iPhone SE)에서 끝부분이 "…"로 잘려 입력이 안 보인다.
            // 남는 높이를 이 칸이 모두 가져가므로 미리보기는 디스플레이 맨 아래에 붙는다.
            ScrollViewReader { proxy in
                ScrollView(.vertical) {
                    VStack(alignment: .trailing, spacing: 0) {
                        HStack(alignment: .center, spacing: 2) {
                            Spacer(minLength: 0)

                            Text(expressionText)
                                .scaledFont(size: expressionFontSize, weight: .light)
                                .foregroundStyle(state.isCalculatedResult ? colors.accent : colors.keyText)
                                .multilineTextAlignment(.trailing)

                            // 커서도 글자 크기 설정을 따라 커진 수식 글자 높이에 맞춘다.
                            CursorBar(
                                color: colors.accent,
                                height: ScaledFont.size(expressionFontSize, for: dynamicTypeSize) * 1.05,
                            )
                        }

                        ConvertedAmountText(
                            amount: state.convertedExpressionAmount,
                            currency: state.selectedExchangeCurrency,
                            colors: colors,
                        )

                        Color.clear
                            .frame(height: 0)
                            .id(Self.expressionBottomID)
                    }
                }
                .scrollIndicators(.hidden)
                .scrollBounceBehavior(.basedOnSize)
                .onChange(of: state.expression) { proxy.scrollTo(Self.expressionBottomID, anchor: .bottom) }
                .onChange(of: state.convertedExpressionAmount) {
                    proxy.scrollTo(Self.expressionBottomID, anchor: .bottom)
                }
            }

            if !state.expression.isEmpty, !state.previewResult.isEmpty {
                Text(CalculatorFormatting.formatWithCommas(state.previewResult))
                    .scaledFont(size: Self.previewFontSize, weight: .medium)
                    .foregroundStyle(colors.preview)
                    .multilineTextAlignment(.trailing)
                    // 큰 글자에서 긴 결과가 두 줄로 넘어가면 디스플레이 높이를 더 가져가 수식 칸이 사라진다.
                    // 한 줄로 두고 넘치면 줄인다.
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)

                ConvertedAmountText(
                    amount: state.convertedPreviewAmount,
                    currency: state.selectedExchangeCurrency,
                    colors: colors,
                )
            }
        }
    }

    private var expressionText: AttributedString {
        CalculatorFormatting.attributedExpression(
            state.expression,
            operatorColor: colors.accent,
        )
    }

    private var expressionFontSize: CGFloat {
        Self.expressionFontSize(forLength: state.expression.count)
    }

    /// 수식 길이에 따른 기본 글자 크기에서의 수식 크기. 길어질수록 줄여 한 줄에 더 들어가게 한다.
    static func expressionFontSize(forLength length: Int) -> CGFloat {
        switch length {
        case 0...11: 42
        case 12...16: 35
        default: 30
        }
    }
}

/// 환산 금액 표시 (Android `ConvertedAmountText` 대응)
private struct ConvertedAmountText: View {
    let amount: String
    let currency: CurrencyInfo?
    let colors: CalculatorColors

    /// 기본 글자 크기에서의 크기. 디스플레이에서 가장 작은 글자라 `CalculatorDisplayView.textScale(for:)`의 기준이다.
    static let fontSize: CGFloat = 14

    var body: some View {
        if !amount.isEmpty, currency != nil {
            Text("≈ \(CalculatorFormatting.formatWithCommas(amount))")
                .scaledFont(size: Self.fontSize, weight: .medium)
                .foregroundStyle(colors.preview)
                .lineLimit(1)
        }
    }
}

/// 수식 끝의 깜빡이는 커서
private struct CursorBar: View {
    let color: Color
    let height: CGFloat

    @State private var isVisible = true

    var body: some View {
        RoundedRectangle(cornerRadius: 1.5)
            .fill(color)
            .frame(width: 2.5, height: height)
            .opacity(isVisible ? 1 : 0)
            .task {
                while !Task.isCancelled {
                    do {
                        try await Task.sleep(for: .milliseconds(500))
                    } catch {
                        // 화면이 사라져 취소되면 깜빡임을 한 번 더 바꾸지 않고 끝낸다.
                        return
                    }
                    withAnimation(.easeInOut(duration: 0.15)) {
                        isVisible.toggle()
                    }
                }
            }
    }
}
