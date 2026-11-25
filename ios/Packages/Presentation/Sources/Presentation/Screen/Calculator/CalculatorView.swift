import Domain
import StoreKit
import SwiftUI

/// 계산기 화면 (Android `CalculatorScreen` 대응)
struct CalculatorView: View {
    let viewModel: CalculatorViewModel
    /// 인앱 리뷰를 요청했을 때 부른다. 리뷰를 부탁한 직후에는 전면 광고를 띄우지 않도록 알린다.
    var onReviewRequested: () -> Void = {}

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.requestReview) private var requestReview
    @State private var showHistory = false
    @State private var snackbar = SnackbarPresenter()
    /// 실패 문구가 차지하는 높이. 문구가 떠 있으면 그만큼 키패드를 줄여 키패드가 탭바 아래로 밀려나지 않게 한다.
    @State private var failureMessageHeight: CGFloat = 0

    var body: some View {
        let colors = CalculatorColors.palette(for: colorScheme)

        // iPad·가로 화면에서 정사각 키가 화면 폭만큼 커지지 않게 내용 폭을 정해 가운데에 둔다. iPhone은 화면 폭 그대로다.
        GeometryReader { geometry in
            let contentWidth = CalculatorLayout.contentWidth(for: geometry.size)
            let failureHeight = viewModel.state.exchangeFailure == nil ? 0 : failureMessageHeight

            calculatorContent(
                colors: colors,
                keypadWidth: CalculatorLayout.keypadWidth(
                    contentWidth: contentWidth,
                    height: geometry.size.height - failureHeight,
                ),
            )
            .frame(width: contentWidth)
            .frame(maxWidth: .infinity)
        }
        .padding(.horizontal, 16)
        .background(colors.background.ignoresSafeArea())
        .snackbarOverlay(snackbar)
        .task {
            for await sideEffect in viewModel.sideEffects() {
                switch sideEffect {
                case let .showSnackbar(message):
                    snackbar.show(message)

                case .requestReview:
                    // 개발 빌드는 요청할 때마다 창이 떠서 XCUITest·Maestro Flow를 막는다.
                    if !UITestingLaunch.isActive {
                        requestReview()
                        onReviewRequested()
                    }
                }
            }
        }
        .toolbar(.hidden, for: .navigationBar)
    }

    private func calculatorContent(colors: CalculatorColors, keypadWidth: CGFloat) -> some View {
        VStack(spacing: 0) {
            CalculatorCurrencySelectorRow(
                state: viewModel.state,
                colors: colors,
                onIntent: { viewModel.send($0) },
            )
            .padding(.top, 4)

            if let failure = viewModel.state.exchangeFailure {
                LoadFailureMessage(message: failure, verticalPadding: 8) {
                    viewModel.send(.retryExchange)
                }
                .accessibilityIdentifier("calculator.failure")
                .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { failureMessageHeight = $0 }
            }

            // 키패드가 크기를 먼저 정하고 남는 높이를 디스플레이가 쓴다(Android의 디스플레이 weight(1f) 대응).
            // 남는 높이가 최소 높이보다 모자란 순간(실패 문구 높이를 재기 전 등)에도 위 칸을 덮지 않게 자른다.
            CalculatorDisplayView(state: viewModel.state, colors: colors)
                .frame(maxHeight: .infinity)
                .padding(.vertical, 16)
                .clipped()

            CalculatorKeypadView(
                colors: colors,
                showHistory: $showHistory,
                histories: viewModel.state.histories,
                onClearHistory: { viewModel.send(.clearHistory) },
                onKeyTap: { key in
                    if key == .history {
                        showHistory.toggle()
                    } else if let intent = key.toIntent() {
                        viewModel.send(intent)
                    }
                },
            )
            // 크기를 화면 크기로만 정해 고정한다. 디스플레이와 높이를 나눠 가지면 수식이 길어질 때마다
            // 키가 작아지며 자리가 움직인다(`CalculatorLayout.keypadWidth` 참조).
            .frame(width: keypadWidth, height: keypadWidth * 5 / 4)
            // 떠 있는 탭바와 붙어 보이지 않게 띄운다. 이 여백은 `reservedHeightOutsideKeypad`에 들어 있다.
            .padding(.bottom, 12)
        }
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

    /// 수식 칸 맨 아래(환산 금액 아래)를 가리키는 스크롤 위치
    private static let expressionBottomID = "calculator.expression.bottom"

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

/// 키패드 + 계산 기록 오버레이 (Android `CalculatorKeypadArea` 대응)
private struct CalculatorKeypadView: View {
    let colors: CalculatorColors
    @Binding var showHistory: Bool
    let histories: [CalculatorState.HistoryItem]
    let onClearHistory: () -> Void
    let onKeyTap: (CalculatorKey) -> Void

    var body: some View {
        VStack(spacing: 0) {
            ForEach(calculatorKeyRows.indices, id: \.self) { rowIndex in
                HStack(spacing: 0) {
                    ForEach(calculatorKeyRows[rowIndex], id: \.self) { key in
                        CalculatorKeyButton(
                            key: key,
                            colors: colors,
                            onTap: { onKeyTap(key) },
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
                        onClearHistory: onClearHistory,

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
    let onTap: () -> Void

    var body: some View {
        keyButton
            .accessibilityIdentifier(key.testID)
    }

    @ViewBuilder
    private var keyButton: some View {
        switch key {
        case .history:
            CalculatorIconButton(
                systemImage: "clock",
                appearance: neutralKey(content: colors.keyText),
                accessibilityLabel: L("계산 기록"),
                action: onTap,
            )

        case .clear:
            CalculatorButton(text: "AC", appearance: destructiveKey, action: onTap)

        case .parenthesis:
            CalculatorButton(text: "( )", appearance: neutralKey(content: colors.keyText), action: onTap)

        case .dot:
            CalculatorButton(text: ".", appearance: neutralKey(content: colors.keyText), action: onTap)

        case .delete:
            DeleteCalculatorButton(appearance: destructiveKey, onDeleteAction: onTap)

        case .calculate:
            CalculatorButton(
                text: "=",
                appearance: coloredKey(content: .white, background: colors.equalsBackground),
                action: onTap,
            )

        case let .number(value):
            CalculatorButton(text: value, appearance: neutralKey(content: colors.keyText), action: onTap)

        case let .operatorKey(displayText, _):
            CalculatorButton(
                text: displayText,
                appearance: neutralKey(content: colors.keyText, background: colors.operatorKeyBackground),
                action: onTap,
            )
        }
    }

    /// 무채색 글자 키. 누르면 글자도 강조색으로 바뀐다.
    private func neutralKey(content: Color, background: Color? = nil) -> CalculatorKeyAppearance {
        CalculatorKeyAppearance(
            backgroundColor: background ?? colors.keyBackground,
            contentColor: content,
            pressedContentColor: colors.accent,
            hasShadow: colors.hasKeyShadow,
            press: colors.keyPress,
        )
    }

    /// 글자·바탕에 이미 색이 있는 키(=). 누를 때 글자색은 그대로 두고 빛과 바탕만 바뀐다.
    private func coloredKey(content: Color, background: Color? = nil) -> CalculatorKeyAppearance {
        CalculatorKeyAppearance(
            backgroundColor: background ?? colors.keyBackground,
            contentColor: content,
            pressedContentColor: nil,
            hasShadow: colors.hasKeyShadow,
            press: colors.keyPress,
        )
    }

    /// AC·⌫. 숫자 키 바탕에 빨간 글자를 두고, 누를 때 번지는 빛도 같은 빨강으로 맞춘다.
    private var destructiveKey: CalculatorKeyAppearance {
        CalculatorKeyAppearance(
            backgroundColor: colors.keyBackground,
            contentColor: colors.destructive,
            pressedContentColor: nil,
            hasShadow: colors.hasKeyShadow,
            press: colors.destructiveKeyPress,
        )
    }
}
