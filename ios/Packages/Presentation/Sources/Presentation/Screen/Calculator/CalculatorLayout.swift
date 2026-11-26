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

    /// 기본 글자 크기에서 키패드 말고 늘 자리를 차지하는 높이:
    /// 통화 선택 줄(48) + 디스플레이 최소 높이(140, 위아래 여백 32 포함) + 키패드 아래 여백(12).
    /// 디스플레이 최소 높이는 수식 한 줄과 환산 금액, 미리보기와 그 환산 금액이 들어가는 높이다.
    static let reservedHeightOutsideKeypad: CGFloat = 200
    /// `reservedHeightOutsideKeypad` 중 디스플레이의 글자가 차지하는 높이(140 - 위아래 여백 32). 글자 크기를 따라 커진다.
    static let displayTextHeight: CGFloat = 108
    /// `displayTextHeight` 중 환산 금액 두 줄(수식 아래, 미리보기 아래)이 차지하는 높이. 14pt 글자 한 줄이 17pt 남짓이다.
    static let convertedAmountsHeight: CGFloat = 34
    /// 실패 문구가 떠 있을 때 키 한 칸의 하한. 키 버튼은 칸 안쪽에 사방 6pt 여백을 두고 그 안의 원만 눌리므로,
    /// 원이 iOS HIG 최소 터치 크기(44)가 되는 56(44 + 6 × 2)으로 둔다.
    static let minimumKeySize: CGFloat = 56

    /// 글자 크기 설정에 맞춘 키패드 밖 높이.
    ///
    /// 디스플레이의 글자 높이만 `textScale`배로 키운다. 통화 선택 줄은 계산기 상한(xxxLarge)에서도 최소 터치 높이(44) 안에
    /// 들어가 늘리지 않는다. 글자를 기본보다 작게 해도 키패드를 기본보다 키우지 않도록 1배 아래로는 줄이지 않는다.
    /// - Parameters:
    ///   - textScale: 디스플레이 글자가 기본 크기보다 커진 비율 (기본 글자 크기에서 1)
    ///   - reservesConvertedAmounts: 환산 금액 두 줄 몫을 남길지. 환율이 없어 환산 금액이 비는 동안은 false로 넘겨
    ///     그 높이를 키패드에 돌린다.
    static func reservedHeightOutsideKeypad(textScale: CGFloat, reservesConvertedAmounts: Bool = true) -> CGFloat {
        let textHeight = reservesConvertedAmounts ? displayTextHeight : displayTextHeight - convertedAmountsHeight

        return reservedHeightOutsideKeypad - displayTextHeight + textHeight * max(1, textScale)
    }

    /// - Parameter size: 계산기 화면이 쓸 수 있는 크기 (좌우 여백을 뺀 값)
    static func contentWidth(for size: CGSize) -> CGFloat {
        let widthForMaxKeypadHeight = size.height * maxKeypadHeightRatio * 4 / 5

        return max(0, min(size.width, maxContentWidth, widthForMaxKeypadHeight))
    }

    /// 키패드 폭. 키는 이 폭의 1/4인 정사각형이다.
    ///
    /// 화면 크기만으로 정하고 디스플레이 내용과는 상관없게 둔다. 키패드가 디스플레이와 높이를 나눠 가지면
    /// 수식이 길어질 때마다 키가 작아지며 자리가 움직여, 빠르게 누르면 다른 키가 눌린다(iPhone SE에서 확인).
    /// 내용 폭을 그대로 쓰되, 높이가 모자란 화면에서는 `reservedHeightOutsideKeypad(textScale:reservesConvertedAmounts:)`를
    /// 남기는 만큼으로 줄인다.
    /// 글자 크기 설정·환율 유무·키 하한도 디스플레이 내용과 달리 입력 중에 바뀌지 않으므로 키 자리를 흔들지 않는다.
    /// - Parameters:
    ///   - contentWidth: `contentWidth(for:)`로 정한 내용 폭
    ///   - height: 계산기 화면이 쓸 수 있는 높이
    ///   - textScale: 디스플레이 글자가 기본 크기보다 커진 비율 (기본 글자 크기에서 1)
    ///   - reservesConvertedAmounts: 환산 금액 두 줄 몫을 남길지
    ///     (`reservedHeightOutsideKeypad(textScale:reservesConvertedAmounts:)` 참조)
    ///   - minimumKeySize: 키 한 칸의 하한. 내용 폭을 넘지 않는다. 하한이 걸리면 디스플레이가 남길 높이보다 줄어 잘린다.
    ///     디스플레이 글자 높이를 다 내줘도 모자라면(가로 화면·작은 iPad 창처럼 낮은 높이) 하한보다 높이를 지킨다.
    ///     키패드가 통화 선택 줄·디스플레이 여백·아래 여백과 함께 `height` 안에 들어가야 탭바 아래로 밀려나지 않는다.
    static func keypadWidth(
        contentWidth: CGFloat,
        height: CGFloat,
        textScale: CGFloat = 1,
        reservesConvertedAmounts: Bool = true,
        minimumKeySize: CGFloat = 0,
    ) -> CGFloat {
        let reserved = reservedHeightOutsideKeypad(
            textScale: textScale,
            reservesConvertedAmounts: reservesConvertedAmounts,
        )
        let widthForAvailableHeight = (height - reserved) * 4 / 5
        // 디스플레이 글자 높이를 0까지 내줬을 때 키패드가 쓸 수 있는 폭. 하한도 이것을 넘지 않는다.
        let widthForFixedHeight = (height - (reservedHeightOutsideKeypad - displayTextHeight)) * 4 / 5

        return max(0, min(contentWidth, widthForFixedHeight, max(minimumKeySize * 4, widthForAvailableHeight)))
    }
}
