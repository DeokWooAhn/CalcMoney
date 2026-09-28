import SwiftUI

/// 인덱스가 차지하는 폭. 목록 행은 이만큼 오른쪽을 비워 두어야 하트 버튼이 가려지지 않는다.
let fastScrollIndexWidth: CGFloat = 32

/// 물결 중심과 라벨 사이 거리에 따른 커지는 정도 (Android `waveInfluence` 대응)
///
/// 물결 중심은 1, 반경 끝은 0이고 그 사이는 코사인 곡선으로 부드럽게 줄어든다.
/// 반경이 0 이하이면 물결이 없다. 거리의 부호는 무시한다.
nonisolated func waveInfluence(distance: CGFloat, radius: CGFloat) -> CGFloat {
    guard radius > 0 else { return 0 }

    let ratio = abs(distance) / radius
    guard ratio < 1 else { return 0 }

    return (cos(ratio * .pi) + 1) / 2
}

/// 물결 중심 주변 라벨이 밀려나고 흐려지는 정도 (Android `waveRipple` 대응)
///
/// 물결 중심과 반경 끝은 0이고, 그 가운데에서 1로 가장 크다. 선택된 라벨 바로 위아래 라벨이 가장 크게 반응해
/// 위아래로 출렁이는 느낌을 준다. 반경이 0 이하이면 물결이 없다. 거리의 부호는 무시한다.
nonisolated func waveRipple(distance: CGFloat, radius: CGFloat) -> CGFloat {
    guard radius > 0 else { return 0 }

    let ratio = abs(distance) / radius
    guard ratio < 1 else { return 0 }

    return sin(ratio * .pi)
}

/// 목록 오른쪽에 붙는 빠른 이동 인덱스 (Android `FastScrollIndex` 대응)
///
/// 라벨을 누르거나 위아래로 끌면 그 위치의 라벨을 `onLabelSelected`로 알린다. 같은 라벨 위에서는 다시 알리지 않는다.
/// 누르는 동안에는 선택된 라벨이 살짝 커지고 위아래 라벨이 흐려지며 밀려나는 물결 효과를 준다. 손가락에 가려진 현재 라벨은
/// 목록 쪽에서 `FastScrollIndexPreview`로 크게 보여 준다.
/// 스크롤 중에만 잠깐 보이는 보조 조작이라 접근성 트리에서는 뺀다. 목록 자체는 그대로 탐색할 수 있다.
struct FastScrollIndex: View {
    let labels: [String]
    let onLabelSelected: (String) -> Void
    /// 인덱스를 누르고 있는지 여부. 누르는 동안에는 인덱스를 숨기지 않아야 한다.
    let onTouchingChange: (Bool) -> Void

    @State private var activeLabel: String?
    /// 드래그가 끝나거나 취소되면 자동으로 `false`로 돌아간다. `onEnded`는 취소 때 불리지 않아 이걸로 정리한다.
    @GestureState private var isDragging = false
    /// 물결 중심이 있는 라벨 위치. 끄는 동안 라벨 사이를 부드럽게 미끄러지고, 손을 떼도 그 자리에서 가라앉는다.
    @State private var waveCenterIndex: CGFloat = 0
    /// 0이면 물결이 없고 1이면 최대. 누르고 뗄 때 애니메이션으로 바뀐다.
    @State private var waveIntensity: CGFloat = 0
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    private let pillWidth: CGFloat = 22
    private let verticalPadding: CGFloat = 6
    private let maxLabelHeight: CGFloat = 18
    /// 선택된 라벨의 최대 배율. 과하지 않게 살짝만 키운다.
    private let waveMaxScale: CGFloat = 1.35
    /// 물결이 퍼지는 반경. 선택된 라벨 위아래로 라벨 몇 개까지 영향을 줄지 정한다.
    private let waveRadiusInLabels: CGFloat = 2.5
    /// 선택된 라벨이 커진 만큼 위아래 라벨을 밀어내는 최대 거리
    private let waveMaxPush: CGFloat = 3
    /// 선택된 라벨 바로 위아래 라벨을 흐리게 하는 정도. 1이면 완전히 사라진다.
    private let waveMaxDim: CGFloat = 0.7

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
            withAnimation(.spring(response: 0.3, dampingFraction: 0.9)) {
                waveIntensity = dragging ? 1 : 0
            }
            onTouchingChange(dragging)
        }
        .sensoryFeedback(.selection, trigger: activeLabel) { _, newLabel in newLabel != nil }
        .accessibilityHidden(true)
    }

    private func pill(labelHeight: CGFloat) -> some View {
        VStack(spacing: 0) {
            ForEach(Array(labels.enumerated()), id: \.element) { index, label in
                let distance = (CGFloat(index) - waveCenterIndex) * labelHeight
                let radius = labelHeight * waveRadiusInLabels
                let motion = reduceMotion ? 0 : waveIntensity
                let ripple = waveRipple(distance: distance, radius: radius)

                labelView(label)
                    .frame(width: pillWidth, height: labelHeight)
                    .scaleEffect(1 + (waveMaxScale - 1) * motion * waveInfluence(distance: distance, radius: radius))
                    .offset(y: (distance < 0 ? -1 : 1) * waveMaxPush * motion * ripple)
                    .opacity(1 - waveMaxDim * waveIntensity * ripple)
            }
        }
        .padding(.vertical, verticalPadding)
        .background(Capsule().fill(AppColors.currencySelectorSurface.opacity(0.92)))
    }

    @ViewBuilder
    private func labelView(_ label: String) -> some View {
        let isActive = label == activeLabel
        let color: Color = isActive ? .primary : .secondary

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
                select(index(atY: value.location.y, labelHeight: labelHeight))
            }
    }

    private func index(atY offsetY: CGFloat, labelHeight: CGFloat) -> Int? {
        guard !labels.isEmpty, labelHeight > 0 else { return nil }

        let index = Int((offsetY - verticalPadding) / labelHeight)

        return min(max(index, 0), labels.count - 1)
    }

    private func select(_ index: Int?) {
        guard let index else { return }

        let label = labels[index]
        guard label != activeLabel else { return }

        let isFirstTouch = activeLabel == nil
        activeLabel = label

        if isFirstTouch {
            // 처음 누를 때는 누른 자리에 바로 물결을 놓는다. 이전 위치에서 미끄러져 오지 않게 한다.
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) { waveCenterIndex = CGFloat(index) }
        } else {
            withAnimation(.spring(response: 0.25, dampingFraction: 0.9)) {
                waveCenterIndex = CGFloat(index)
            }
        }
        onLabelSelected(label)
    }
}

/// 누르고 있는 인덱스 라벨을 목록 가운데에 크게 보여 주는 미리보기 (Android `FastScrollIndexPreview` 대응)
///
/// 인덱스 라벨은 손가락에 가려지므로, 지금 어느 구간으로 가는지 여기서 확인하게 한다.
struct FastScrollIndexPreview: View {
    let label: String

    var body: some View {
        Group {
            if label == favoriteIndexLabel {
                Image(systemName: "star.fill")
                    .font(.system(size: 34, weight: .bold))
            } else {
                Text(label)
                    .font(.system(size: 36, weight: .bold))
            }
        }
        .foregroundStyle(.white.opacity(0.9))
        .frame(width: 80, height: 80)
        .background(RoundedRectangle(cornerRadius: 18).fill(.black.opacity(0.72)))
        .accessibilityHidden(true)
    }
}
