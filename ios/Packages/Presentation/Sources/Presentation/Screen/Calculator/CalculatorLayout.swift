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

    /// 키패드 말고 늘 자리를 차지하는 높이: 통화 선택 줄(48) + 디스플레이 최소 높이(140, 위아래 여백 포함) + 키패드 아래 여백(12).
    /// 디스플레이 최소 높이는 수식 한 줄과 환산 금액, 미리보기와 그 환산 금액이 들어가는 높이다.
    static let reservedHeightOutsideKeypad: CGFloat = 200

    /// - Parameter size: 계산기 화면이 쓸 수 있는 크기 (좌우 여백을 뺀 값)
    static func contentWidth(for size: CGSize) -> CGFloat {
        let widthForMaxKeypadHeight = size.height * maxKeypadHeightRatio * 4 / 5

        return max(0, min(size.width, maxContentWidth, widthForMaxKeypadHeight))
    }

    /// 키패드 폭. 키는 이 폭의 1/4인 정사각형이다.
    ///
    /// 화면 크기만으로 정하고 디스플레이 내용과는 상관없게 둔다. 키패드가 디스플레이와 높이를 나눠 가지면
    /// 수식이 길어질 때마다 키가 작아지며 자리가 움직여, 빠르게 누르면 다른 키가 눌린다(iPhone SE에서 확인).
    /// 내용 폭을 그대로 쓰되, 높이가 모자란 화면에서는 `reservedHeightOutsideKeypad`를 남기는 만큼으로 줄인다.
    /// - Parameters:
    ///   - contentWidth: `contentWidth(for:)`로 정한 내용 폭
    ///   - height: 계산기 화면이 쓸 수 있는 높이
    static func keypadWidth(contentWidth: CGFloat, height: CGFloat) -> CGFloat {
        let widthForAvailableHeight = (height - reservedHeightOutsideKeypad) * 4 / 5

        return max(0, min(contentWidth, widthForAvailableHeight))
    }
}
