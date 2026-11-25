import SwiftUI

/// 실패 문구·아이콘·다시 시도를 묶은 공통 표시 (Android `LoadFailureMessage` 대응)
struct LoadFailureMessage: View {
    let message: String
    /// 재시도를 포함해 관련 요청이 진행 중이면 true.
    /// 버튼을 비활성화하고 진행 표시를 보여 준다. 연타로 요청이 여러 번 나가는 것도 막는다.
    var isRetrying = false
    /// 위아래 여백. 키패드와 높이를 나누는 계산기는 줄여서 키가 덜 작아지게 한다.
    var verticalPadding: CGFloat = 24
    /// 아이콘을 보일지. 키패드와 높이를 나누는 계산기는 빼서 문구를 낮춘다.
    var showsIcon = true
    let onRetry: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            if showsIcon {
                Image(systemName: "exclamationmark.circle")
                    .foregroundStyle(.secondary)
            }

            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                // 계산기처럼 높이를 다른 칸과 나누는 화면에서 문구가 한 줄로 눌려 "…"로 잘리지 않게 한다.
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: 8) {
                if isRetrying {
                    // 버튼 안에 두면 비활성 버튼의 회색을 따라가 `tint`가 먹지 않고, 작은 표시가 거의 보이지 않는다.
                    ProgressView()
                        .controlSize(.small)
                        .tint(Color.accentColor)
                }

                Button(L("다시 시도"), action: onRetry)
                    .font(.subheadline)
                    .disabled(isRetrying)
                    .accessibilityIdentifier("failure.retry")
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 16)
        .padding(.vertical, verticalPadding)
        // 묶음 요소로 두지 않으면 호출부가 붙이는 id(`favorite.failure` 등)가 안쪽 요소의 id를 모두 덮어
        // Maestro에서 `failure.retry`를 찾지 못한다.
        .accessibilityElement(children: .contain)
    }
}
