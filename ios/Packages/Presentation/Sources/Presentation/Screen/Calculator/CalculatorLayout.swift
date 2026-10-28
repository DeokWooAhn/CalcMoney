import CoreGraphics

/// 계산기 화면의 내용(수식 영역 + 키패드) 폭을 정한다.
///
/// 키패드는 정사각 키 4열 × 5행이라 폭이 정해지면 높이(폭 × 5/4)도 정해진다.
/// iPhone은 화면 폭을 그대로 쓴다. iPad처럼 넓은 화면이나 가로로 눕힌 화면에서 폭을 그대로 쓰면
/// 키가 너무 커져 수식 영역이 사라지므로, 최대 폭과 화면 높이로 정한 상한 중 작은 값으로 줄인다.
enum CalculatorLayout {
    /// 내용의 최대 폭. iPad에서 키 하나가 120pt 안팎이 된다.
    static let maxContentWidth: CGFloat = 480
    /// 키패드가 화면 높이에서 차지할 수 있는 최대 비율. 나머지는 통화 선택 줄과 수식 영역이 쓴다.
    static let maxKeypadHeightRatio: CGFloat = 0.75

    /// - Parameter size: 계산기 화면이 쓸 수 있는 크기 (좌우 여백을 뺀 값)
    static func contentWidth(for size: CGSize) -> CGFloat {
        let widthForMaxKeypadHeight = size.height * maxKeypadHeightRatio * 4 / 5

        return max(0, min(size.width, maxContentWidth, widthForMaxKeypadHeight))
    }
}
