import SwiftUI

/// 원형 계산기 버튼 (Android `CalculatorButton` 대응)
struct CalculatorButton: View {
    let text: String
    let backgroundColor: Color
    let textColor: Color
    let hasShadow: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: 30, weight: .medium))
                .foregroundStyle(textColor)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(backgroundColor)
                .clipShape(Circle())
                .calculatorKeyShadow(enabled: hasShadow)
        }
        .buttonStyle(CalculatorKeyButtonStyle())
        .aspectRatio(1, contentMode: .fit)
        .padding(6)
        .accessibilityIdentifier("keypad.\(text)")
    }
}

/// 아이콘 계산기 버튼 (Android `CalculatorIconButton` 대응)
struct CalculatorIconButton: View {
    let systemImage: String
    let backgroundColor: Color
    let iconColor: Color
    let hasShadow: Bool
    let accessibilityLabel: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: 24, weight: .medium))
                .foregroundStyle(iconColor)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(backgroundColor)
                .clipShape(Circle())
                .calculatorKeyShadow(enabled: hasShadow)
        }
        .buttonStyle(CalculatorKeyButtonStyle())
        .aspectRatio(1, contentMode: .fit)
        .padding(6)
        .accessibilityLabel(accessibilityLabel)
    }
}

/// 길게 누르면 반복 삭제되는 버튼 (Android `DeleteCalculatorButton` 대응)
struct DeleteCalculatorButton: View {
    let backgroundColor: Color
    let textColor: Color
    let hasShadow: Bool
    let onDeleteAction: () -> Void

    @State private var repeatTask: Task<Void, Never>?
    @State private var isPressed = false

    var body: some View {
        Text("⌫")
            .font(.system(size: 31, weight: .medium))
            .foregroundStyle(textColor)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(isPressed ? backgroundColor.opacity(0.7) : backgroundColor)
            .clipShape(Circle())
            .calculatorKeyShadow(enabled: hasShadow)
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
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.7 : 1.0)
    }
}

private extension View {
    func calculatorKeyShadow(enabled: Bool) -> some View {
        shadow(color: enabled ? .black.opacity(0.12) : .clear, radius: 3, x: 0, y: 1)
    }
}
