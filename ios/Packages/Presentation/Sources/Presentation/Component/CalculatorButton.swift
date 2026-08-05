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
