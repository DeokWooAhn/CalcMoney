import Domain
import SwiftUI

/// 계산기 화면 (Android `CalculatorScreen` 대응)
struct CalculatorView: View {
    let viewModel: CalculatorViewModel

    @Environment(\.colorScheme) private var colorScheme
    @State private var showHistory = false
    @State private var snackbar = SnackbarPresenter()

    var body: some View {
        let colors = CalculatorColors.palette(for: colorScheme)

        VStack(spacing: 0) {
            CalculatorCurrencySelectorRow(
                state: viewModel.state,
                colors: colors,
                onIntent: { viewModel.send($0) },
            )
            .padding(.top, 4)

            CalculatorDisplayView(state: viewModel.state, colors: colors)
                .frame(maxHeight: .infinity)
                .padding(.vertical, 16)

            CalculatorKeypadView(
                colors: colors,
                showHistory: $showHistory,
                histories: viewModel.state.histories,
                onIntent: { viewModel.send($0) },
            )
        }
        .padding(.horizontal, 16)
        .background(colors.background.ignoresSafeArea())
        .snackbarOverlay(snackbar)
        .task {
            for await sideEffect in viewModel.sideEffects() {
                switch sideEffect {
                case let .showSnackbar(message):
                    snackbar.show(message)
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }
}

/// 상단 통화 선택 줄 (Android `CalculatorCurrencySelectorRow` 대응)
private struct CalculatorCurrencySelectorRow: View {
    let state: CalculatorState
    let colors: CalculatorColors
    let onIntent: (CalculatorIntent) -> Void

    var body: some View {
        HStack(spacing: 8) {
            CurrencySelectorView(
                selectedCurrency: state.mainExchangeCurrency,
                availableCurrencies: state.availableCurrencies,
                favoriteCurrencyCodes: state.favoriteCurrencyCodes,
                dialogTitle: L("메인 환율 선택"),
                compact: true,
                onCurrencySelected: { onIntent(.selectMainExchangeCurrency($0)) },
                onToggleFavorite: { onIntent(.toggleFavorite($0)) },
            )

            Button {
                onIntent(.swapExchangeCurrencies)
            } label: {
                Image(systemName: "arrow.left.arrow.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(colors.keyText)
                    .frame(width: 36, height: 36)
                    .background(colors.keyBackground)
                    .clipShape(RoundedRectangle(cornerRadius: 18))
                    // 원의 지름은 36pt로 두고 터치 영역만 iOS HIG 최소치인 44pt로 넓힌다.
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(L("환율 통화 교환"))

            CurrencySelectorView(
                selectedCurrency: state.selectedExchangeCurrency,
                availableCurrencies: state.availableCurrencies,
                favoriteCurrencyCodes: state.favoriteCurrencyCodes,
                dialogTitle: L("보조 환율 선택"),
                compact: true,
                onCurrencySelected: { onIntent(.selectExchangeCurrency($0)) },
                onToggleFavorite: { onIntent(.toggleFavorite($0)) },
            )
        }
        .frame(maxWidth: .infinity)
    }
}

/// 환산 금액 표시 (Android `ConvertedAmountText` 대응)
private struct ConvertedAmountText: View {
    let amount: String
    let currency: CurrencyInfo?
    let colors: CalculatorColors

    var body: some View {
        if !amount.isEmpty, currency != nil {
            Text("≈ \(CalculatorFormatting.formatWithCommas(amount))")
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(colors.preview)
                .lineLimit(1)
        }
    }
}

/// 수식·미리보기 표시 영역 (Android `CalculatorDisplay` 대응)
private struct CalculatorDisplayView: View {
    let state: CalculatorState
    let colors: CalculatorColors

    var body: some View {
        VStack(alignment: .trailing, spacing: 0) {
            HStack(alignment: .center, spacing: 2) {
                Spacer(minLength: 0)

                Text(expressionText)
                    .font(.system(size: expressionFontSize, weight: .light))
                    .foregroundStyle(state.isCalculatedResult ? colors.accent : colors.keyText)
                    .multilineTextAlignment(.trailing)

                CursorBar(color: colors.accent, height: expressionFontSize * 1.05)
            }

            ConvertedAmountText(
                amount: state.convertedExpressionAmount,
                currency: state.selectedExchangeCurrency,
                colors: colors,
            )

            Spacer(minLength: 0)

            if !state.expression.isEmpty, !state.previewResult.isEmpty {
                Text(CalculatorFormatting.formatWithCommas(state.previewResult))
                    .font(.system(size: 30, weight: .medium))
                    .foregroundStyle(colors.preview)
                    .multilineTextAlignment(.trailing)

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
        switch state.expression.count {
        case 0...11: 42
        case 12...16: 35
        default: 30
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
                    try? await Task.sleep(for: .milliseconds(500))
                    withAnimation(.easeInOut(duration: 0.15)) {
                        isVisible.toggle()
                    }
                }
            }
    }
}

/// 계산기 키 정의 (Android `CalculatorKey` 대응)
private enum CalculatorKey: Hashable {
    case history
    case clear
    case parenthesis
    case dot
    case delete
    case calculate
    case number(String)
    case operatorKey(displayText: String, inputValue: String)
}

private let calculatorKeyRows: [[CalculatorKey]] = [
    [.history, .clear, .parenthesis, .operatorKey(displayText: "÷", inputValue: "÷")],
    [.number("7"), .number("8"), .number("9"), .operatorKey(displayText: "×", inputValue: "×")],
    [.number("4"), .number("5"), .number("6"), .operatorKey(displayText: "−", inputValue: "-")],
    [.number("1"), .number("2"), .number("3"), .operatorKey(displayText: "+", inputValue: "+")],
    [.dot, .number("0"), .delete, .calculate],
]

/// 키패드 + 계산 기록 오버레이 (Android `CalculatorKeypadArea` 대응)
private struct CalculatorKeypadView: View {
    let colors: CalculatorColors
    @Binding var showHistory: Bool
    let histories: [CalculatorState.HistoryItem]
    let onIntent: (CalculatorIntent) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ForEach(calculatorKeyRows.indices, id: \.self) { rowIndex in
                HStack(spacing: 0) {
                    ForEach(calculatorKeyRows[rowIndex], id: \.self) { key in
                        CalculatorKeyButton(
                            key: key,
                            colors: colors,
                            onHistoryTap: { showHistory.toggle() },
                            onIntent: onIntent,
                        )
                    }
                }
            }
        }
        .overlay(alignment: .topLeading) {
            if showHistory {
                GeometryReader { geometry in
                    let buttonSize = geometry.size.width / 4

                    CalculatorHistoryPanel(
                        histories: histories,
                        colors: colors,
                        onClearHistory: { onIntent(.clearHistory) },
                        onDismiss: { showHistory = false },
                    )
                    .frame(width: buttonSize * 3.5, height: buttonSize * 4 - 12)
                    .offset(y: buttonSize)
                }
                .transition(.move(edge: .leading).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.2), value: showHistory)
    }
}

private struct CalculatorKeyButton: View {
    let key: CalculatorKey
    let colors: CalculatorColors
    let onHistoryTap: () -> Void
    let onIntent: (CalculatorIntent) -> Void

    var body: some View {
        switch key {
        case .history:
            CalculatorIconButton(
                systemImage: "clock",
                backgroundColor: colors.keyBackground,
                iconColor: colors.keyText,
                hasShadow: colors.hasKeyShadow,
                accessibilityLabel: L("계산 기록"),
                action: onHistoryTap,
            )

        case .clear:
            CalculatorButton(
                text: "AC",
                backgroundColor: colors.keyBackground,
                textColor: colors.destructive,
                hasShadow: colors.hasKeyShadow,
                action: { onIntent(.clear) },
            )

        case .parenthesis:
            CalculatorButton(
                text: "( )",
                backgroundColor: colors.keyBackground,
                textColor: colors.keyText,
                hasShadow: colors.hasKeyShadow,
                action: { onIntent(.input(.parenthesis)) },
            )

        case .dot:
            CalculatorButton(
                text: ".",
                backgroundColor: colors.keyBackground,
                textColor: colors.keyText,
                hasShadow: colors.hasKeyShadow,
                action: { onIntent(.input(.dot)) },
            )

        case .delete:
            DeleteCalculatorButton(
                backgroundColor: colors.keyBackground,
                textColor: colors.destructive,
                hasShadow: colors.hasKeyShadow,
                onDeleteAction: { onIntent(.delete) },
            )

        case .calculate:
            CalculatorButton(
                text: "=",
                backgroundColor: colors.equalsBackground,
                textColor: .white,
                hasShadow: colors.hasKeyShadow,
                action: { onIntent(.calculate) },
            )

        case let .number(value):
            CalculatorButton(
                text: value,
                backgroundColor: colors.keyBackground,
                textColor: colors.keyText,
                hasShadow: colors.hasKeyShadow,
                action: { onIntent(.input(.number(value))) },
            )

        case let .operatorKey(displayText, inputValue):
            CalculatorButton(
                text: displayText,
                backgroundColor: colors.operatorKeyBackground,
                textColor: colors.keyText,
                hasShadow: colors.hasKeyShadow,
                action: { onIntent(.input(.operator(inputValue))) },
            )
        }
    }
}
