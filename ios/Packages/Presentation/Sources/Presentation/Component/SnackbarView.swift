import SwiftUI

/// 하단 스낵바 (Android `CustomSnackbarHost` 대응)
struct SnackbarView: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.system(size: 14, weight: .medium))
            .foregroundStyle(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(Color(hex: 0x323234))
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

/// 스낵바 표시 상태를 관리하는 헬퍼. 표시 후 2초 뒤 자동으로 사라진다.
@MainActor
@Observable
final class SnackbarPresenter {
    private(set) var message: String?
    @ObservationIgnored private var dismissTask: Task<Void, Never>?

    func show(_ message: String) {
        dismissTask?.cancel()

        withAnimation(.easeOut(duration: 0.2)) {
            self.message = message
        }

        dismissTask = Task {
            try? await Task.sleep(for: .seconds(2))
            guard !Task.isCancelled else { return }

            withAnimation(.easeIn(duration: 0.2)) {
                self.message = nil
            }
        }
    }
}
