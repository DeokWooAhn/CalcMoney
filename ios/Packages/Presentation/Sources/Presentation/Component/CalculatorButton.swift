import SwiftUI

/// 계산기 키 하나의 색 (Android `CalculatorButton`의 색 인자 대응)
struct CalculatorKeyAppearance {
    let backgroundColor: Color
    let contentColor: Color
    /// 누를 때 글자·아이콘 색. nil이면 바꾸지 않는다(AC·⌫·= 처럼 이미 색이 있는 키).
    var pressedContentColor: Color?
    let hasShadow: Bool
    let press: CalculatorKeyPress
}

/// 원형 계산기 버튼 (Android `CalculatorButton` 대응)
struct CalculatorButton: View {
    let text: String
    let appearance: CalculatorKeyAppearance
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 30, weight: .medium))
        }
        .buttonStyle(CalculatorKeyButtonStyle(appearance: appearance))
        .aspectRatio(1, contentMode: .fit)
        .padding(6)
    }
}

/// 아이콘 계산기 버튼 (Android `CalculatorIconButton` 대응)
struct CalculatorIconButton: View {
    let systemImage: String
    let appearance: CalculatorKeyAppearance
    let accessibilityLabel: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 24, weight: .medium))
        }
        .buttonStyle(CalculatorKeyButtonStyle(appearance: appearance))
        .aspectRatio(1, contentMode: .fit)
        .padding(6)
        .accessibilityLabel(accessibilityLabel)
    }
}

/// 길게 누르면 반복 삭제되는 버튼 (Android `DeleteCalculatorButton` 대응)
struct DeleteCalculatorButton: View {
    let appearance: CalculatorKeyAppearance
    let onDeleteAction: () -> Void

    @State private var repeatTask: Task<Void, Never>?
    @State private var isPressed = false

    var body: some View {
        CalculatorKeyBody(
            label: Text("⌫").font(.system(size: 31, weight: .medium)),
            isPressed: isPressed,
            appearance: appearance,
        )
        .aspectRatio(1, contentMode: .fit)
        .padding(6)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in startRepeatingIfNeeded() }
                .onEnded { _ in stopRepeating() },
        )
        .accessibilityLabel(L("지우기"))
        // 다른 키와 달리 Button이 아니라 Text + DragGesture라 VoiceOver가 버튼으로 읽지 않는다.
        // 게다가 VoiceOver 활성화(더블 탭)는 DragGesture를 전달하지 않아 삭제 자체가 불가능하다.
        // 반복 삭제(길게 누르기)는 제스처에 그대로 두고, 활성화 시 한 글자 삭제를 별도로 연결한다.
        .accessibilityAddTraits(.isButton)
        .accessibilityAction { onDeleteAction() }
        // 손가락을 뗀 시점에만 반복을 멈추면, 누른 채로 화면이 사라질 때(예: 다른 손가락으로 탭 전환)
        // onEnded가 오지 않아 반복 Task가 영원히 남는다. 화면이 사라질 때도 확실히 정리한다.
        .onDisappear { stopRepeating() }
    }

    private func startRepeatingIfNeeded() {
        guard repeatTask == nil else { return }

        isPressed = true
        onDeleteAction()
        repeatTask = Task {
            try? await Task.sleep(for: .milliseconds(500))
            while !Task.isCancelled {
                onDeleteAction()
                try? await Task.sleep(for: .milliseconds(100))
            }
        }
    }

    private func stopRepeating() {
        repeatTask?.cancel()
        repeatTask = nil
        isPressed = false
    }
}

private struct CalculatorKeyButtonStyle: ButtonStyle {
    let appearance: CalculatorKeyAppearance

    func makeBody(configuration: Configuration) -> some View {
        CalculatorKeyBody(label: configuration.label, isPressed: configuration.isPressed, appearance: appearance)
    }
}

/// 계산기 키의 원형 바탕과 누름 효과 (Android `CalculatorKeySurface` 대응)
///
/// 누르면 원 둘레로 강조색 빛이 번지고 바탕이 강조색 쪽으로 물든다. 삼성 계산기의 누름 효과를 앱 강조색으로 옮긴 것이다.
/// 빠르게 켜고 천천히 끄되, 짧게 탭해도 빛이 끝까지 켜졌다가 사라지도록 켜는 애니메이션이 끝난 뒤에 끈다.
private struct CalculatorKeyBody<Label: View>: View {
    let label: Label
    let isPressed: Bool
    let appearance: CalculatorKeyAppearance

    /// 삼성 계산기에서 잰 값: 켜질 때 약 0.1초, 꺼질 때 약 0.25~0.3초
    private static var pressInDuration: Double { 0.09 }
    private static var pressOutDuration: Double { 0.32 }
    /// 빛을 키보다 조금 크게 그린 뒤 흐린다. 키 사이 간격(12pt)으로 번지고, 더 번진 부분은 옆 키에 가려진다.
    private static var glowSpread: CGFloat { 3 }
    private static var glowBlurRadius: CGFloat { 8 }

    @State private var progress: Double = 0
    @State private var pressedAt: Date?

    var body: some View {
        let press = appearance.press

        label
            .foregroundStyle(appearance.contentColor)
            .overlay {
                if let pressedContentColor = appearance.pressedContentColor {
                    label
                        .foregroundStyle(pressedContentColor)
                        .opacity(progress)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background {
                Circle()
                    .fill(appearance.backgroundColor)
                    .overlay(Circle().fill(press.tint.opacity(press.tintAmount * progress)))
            }
            .clipShape(Circle())
            .shadow(color: appearance.hasShadow ? .black.opacity(0.12) : .clear, radius: 3, x: 0, y: 1)
            .background {
                Circle()
                    .fill(AngularGradient(colors: press.glowGradient, center: .center))
                    .padding(-Self.glowSpread)
                    .blur(radius: Self.glowBlurRadius)
                    .opacity(press.glowOpacity * progress)
            }
            .onChange(of: isPressed) { _, pressed in
                if pressed {
                    pressedAt = Date()
                    withAnimation(.easeOut(duration: Self.pressInDuration)) { progress = 1 }
                } else {
                    let elapsed = pressedAt.map { Date().timeIntervalSince($0) } ?? Self.pressInDuration
                    let remainingPressIn = max(0, Self.pressInDuration - elapsed)
                    withAnimation(.easeOut(duration: Self.pressOutDuration).delay(remainingPressIn)) { progress = 0 }
                }
            }
    }
}
