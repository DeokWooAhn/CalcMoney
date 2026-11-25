import Domain
import StoreKit
import SwiftUI

/// 계산기 화면 (Android `CalculatorScreen` 대응)
struct CalculatorView: View {
    let viewModel: CalculatorViewModel
    /// 인앱 리뷰를 요청했을 때 부른다. 리뷰를 부탁한 직후에는 전면 광고를 띄우지 않도록 알린다.
    var onReviewRequested: () -> Void = {}

    /// 계산기 화면의 글자 크기 상한. 앱 전체 상한(accessibility2)보다 낮게 묶는다.
    /// 키가 화면 폭에 맞춘 정사각형이라 접근성 크기에서는 키 글자가 키를 넘치고, 수식 칸이 키패드에 밀려 사라진다.
    /// 여기서 연 통화 선택 시트도 이 상한을 물려받는다.
    static let maxDynamicTypeSize: DynamicTypeSize = .xxxLarge

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
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
            let isShowingFailure = viewModel.state.exchangeFailure != nil
            let failureHeight = isShowingFailure ? failureMessageHeight : 0
            // 아래 `.dynamicTypeSize` 상한은 이 뷰의 자식에만 걸리므로, 여기서 읽은 값은 직접 상한으로 묶는다.
            let textScale = CalculatorDisplayView.textScale(for: min(dynamicTypeSize, Self.maxDynamicTypeSize))

            calculatorContent(
                colors: colors,
                keypadWidth: CalculatorLayout.keypadWidth(
                    contentWidth: contentWidth,
                    height: geometry.size.height - failureHeight,
                    textScale: textScale,
                    // 실패 상태에서 환율이 없으면 환산 금액 두 줄이 비므로 그 높이를 키패드에 돌린다.
                    // 정상 상태에서는 환율을 받는 중(0)에도 남겨, 통화를 바꿀 때마다 키 크기가 바뀌지 않게 한다.
                    reservesConvertedAmounts: !isShowingFailure || viewModel.state.exchangeRate > 0,
                    // 실패 문구가 높이를 가져가도 키가 이보다 작아지지 않게 한다. 모자란 높이는 디스플레이가 잘려 내준다.
                    minimumKeySize: isShowingFailure ? CalculatorLayout.minimumKeySize : 0,
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
        .dynamicTypeSize(...Self.maxDynamicTypeSize)
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
                // 키패드와 높이를 나누므로 아이콘을 빼고 여백을 줄여, 큰 글자에서도 키가 덜 작아지게 한다.
                LoadFailureMessage(message: failure, verticalPadding: 4, showsIcon: false) {
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
                    .scaledFont(size: 14, weight: .semibold)
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
