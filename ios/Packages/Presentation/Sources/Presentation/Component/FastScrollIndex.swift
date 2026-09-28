import SwiftUI

/// 인덱스가 차지하는 폭. 목록 행은 이만큼 오른쪽을 비워 두어야 하트 버튼이 가려지지 않는다.
let fastScrollIndexWidth: CGFloat = 32

/// 목록 오른쪽에 붙는 빠른 이동 인덱스 (Android `FastScrollIndex` 대응)
///
/// 라벨을 누르거나 위아래로 끌면 그 위치의 라벨을 `onLabelSelected`로 알린다. 같은 라벨 위에서는 다시 알리지 않는다.
/// 스크롤 중에만 잠깐 보이는 보조 조작이라 접근성 트리에서는 뺀다. 목록 자체는 그대로 탐색할 수 있다.
struct FastScrollIndex: View {
    let labels: [String]
    let onLabelSelected: (String) -> Void
    /// 인덱스를 누르고 있는지 여부. 누르는 동안에는 인덱스를 숨기지 않아야 한다.
    let onTouchingChange: (Bool) -> Void

    @State private var activeLabel: String?
    /// 드래그가 끝나거나 취소되면 자동으로 `false`로 돌아간다. `onEnded`는 취소 때 불리지 않아 이걸로 정리한다.
    @GestureState private var isDragging = false

    private let pillWidth: CGFloat = 22
    private let verticalPadding: CGFloat = 6
    private let maxLabelHeight: CGFloat = 18

    var body: some View {
        GeometryReader { geometry in
            // 라벨이 많거나 목록이 짧으면 라벨 높이를 줄여 전부 들어가게 한다.
            let labelHeight = min(
                maxLabelHeight,
                (geometry.size.height - verticalPadding * 2) / CGFloat(max(labels.count, 1)),
            )

            pill(labelHeight: labelHeight)
                .frame(width: fastScrollIndexWidth, height: labelHeight * CGFloat(labels.count) + verticalPadding * 2)
                .contentShape(Rectangle())
                .gesture(dragGesture(labelHeight: labelHeight))
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .trailing)
        }
        .frame(width: fastScrollIndexWidth)
        .onChange(of: isDragging) { _, dragging in
            if !dragging {
                activeLabel = nil
            }
            onTouchingChange(dragging)
        }
        .sensoryFeedback(.selection, trigger: activeLabel) { _, newLabel in newLabel != nil }
        .accessibilityHidden(true)
    }

    private func pill(labelHeight: CGFloat) -> some View {
        VStack(spacing: 0) {
            ForEach(labels, id: \.self) { label in
                labelView(label)
                    .frame(width: pillWidth, height: labelHeight)
            }
        }
        .padding(.vertical, verticalPadding)
        .background(Capsule().fill(AppColors.currencySelectorSurface.opacity(0.92)))
    }

    @ViewBuilder
    private func labelView(_ label: String) -> some View {
        let isActive = label == activeLabel
        let color: Color = isActive ? .accentColor : .secondary

        if label == favoriteIndexLabel {
            Image(systemName: "star.fill")
                .font(.system(size: 10))
                .foregroundStyle(color)
        } else {
            Text(label)
                .font(.system(size: 11, weight: isActive ? .bold : .medium))
                .foregroundStyle(color)
        }
    }

    private func dragGesture(labelHeight: CGFloat) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .updating($isDragging) { _, state, _ in
                state = true
            }
            .onChanged { value in
                select(label(atY: value.location.y, labelHeight: labelHeight))
            }
    }

    private func label(atY offsetY: CGFloat, labelHeight: CGFloat) -> String? {
        guard !labels.isEmpty, labelHeight > 0 else { return nil }

        let index = Int((offsetY - verticalPadding) / labelHeight)

        return labels[min(max(index, 0), labels.count - 1)]
    }

    private func select(_ label: String?) {
        guard let label, label != activeLabel else { return }

        activeLabel = label
        onLabelSelected(label)
    }
}
