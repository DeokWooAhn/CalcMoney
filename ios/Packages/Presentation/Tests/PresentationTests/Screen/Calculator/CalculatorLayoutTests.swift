import CoreGraphics
import Testing
import UIKit
@testable import Presentation

@Suite("CalculatorLayout")
struct CalculatorLayoutTests {
    /// iPhone SE 세로에서 계산기 화면이 쓸 수 있는 높이
    private static let seHeight: CGFloat = 561
    /// iPhone SE 세로에서 계산기 화면이 쓸 수 있는 폭 (좌우 여백을 뺀 값)
    private static let seWidth: CGFloat = 343
    /// 계산기의 실패 문구(아이콘 없음, 위아래 여백 4)가 SE 폭·xxxLarge에서 차지하는 높이.
    /// `UIHostingController.sizeThatFits`로 잰 값(92.7, 118.7)을 올림했다. 두 줄은 "잠시 후 다시 시도" 문구,
    /// 세 줄은 "인터넷 연결이 없어" 문구다. `@Test` 인자로 MainActor 밖에서 읽으므로 `nonisolated`로 둔다.
    private nonisolated static let twoLineFailureHeight: CGFloat = 93
    private nonisolated static let threeLineFailureHeight: CGFloat = 119

    @Test("iPhone 세로 화면은 화면 폭을 그대로 쓴다", arguments: [
        CGSize(width: 370, height: 700), // iPhone 17 Pro
        CGSize(width: 343, height: 580), // iPhone SE
    ])
    func iPhone_세로_화면은_화면_폭을_그대로_쓴다(size: CGSize) {
        #expect(CalculatorLayout.contentWidth(for: size) == size.width)
    }

    @Test("iPad 세로처럼 넓고 높은 화면은 최대 폭으로 줄인다")
    func 넓고_높은_화면은_최대_폭으로_줄인다() {
        let width = CalculatorLayout.contentWidth(for: CGSize(width: 1000, height: 1250))

        #expect(width == CalculatorLayout.maxContentWidth)
    }

    @Test("가로로 눕혀 높이가 낮으면 키패드가 높이의 75%를 넘지 않게 폭을 줄인다")
    func 높이가_낮으면_키패드_높이_상한에_맞춰_줄인다() {
        let width = CalculatorLayout.contentWidth(for: CGSize(width: 1000, height: 500))

        #expect(width == 300)
        #expect(width * 5 / 4 <= 500 * CalculatorLayout.maxKeypadHeightRatio)
    }

    @Test("크기가 0이어도 음수 폭을 돌려주지 않는다")
    func 크기가_0이어도_음수_폭을_돌려주지_않는다() {
        #expect(CalculatorLayout.contentWidth(for: .zero) == 0)
    }

    @Test("높이가 넉넉하면 키패드는 내용 폭을 그대로 쓴다")
    func 높이가_넉넉하면_키패드는_내용_폭을_그대로_쓴다() {
        // iPhone 17 Pro 세로: 남는 높이로 정한 폭(417.6)이 내용 폭보다 크다
        #expect(CalculatorLayout.keypadWidth(contentWidth: 370, height: 722) == 370)
    }

    @Test("높이가 모자라면 키패드 밖 높이를 남기는 만큼으로 키패드를 줄인다")
    func 높이가_모자라면_키패드_밖_높이를_남기도록_줄인다() {
        // iPhone SE 세로
        let height: CGFloat = 561
        let width = CalculatorLayout.keypadWidth(contentWidth: 343, height: height)

        #expect(width < 343)
        #expect(abs(width * 5 / 4 + CalculatorLayout.reservedHeightOutsideKeypad - height) < 0.001)
    }

    /// 키패드 밖 높이(200)의 직전·일치·직후, 0·음수 높이, 내용 폭 0, 무한 높이
    @Test("경계 입력에서 키패드 폭은 0 아래로 내려가지 않고 내용 폭을 넘지 않는다", arguments: [
        (contentWidth: CGFloat(343), height: CGFloat(0), expected: CGFloat(0)),
        (contentWidth: 343, height: -50, expected: 0),
        (contentWidth: 343, height: 199, expected: 0),
        (contentWidth: 343, height: 200, expected: 0),
        (contentWidth: 343, height: 201, expected: 0.8),
        (contentWidth: 0, height: 1000, expected: 0),
        // 무한 높이는 높이 제한이 없다는 뜻으로 보고 내용 폭을 그대로 쓴다
        (contentWidth: 343, height: .infinity, expected: 343),
    ])
    func 경계_입력에서_키패드_폭(contentWidth: CGFloat, height: CGFloat, expected: CGFloat) {
        let width = CalculatorLayout.keypadWidth(contentWidth: contentWidth, height: height)

        #expect(abs(width - expected) < 0.001)
    }

    @Test("기본 글자 크기에서는 키패드 밖 높이가 그대로다")
    func 기본_글자_크기에서는_키패드_밖_높이가_그대로다() {
        let reserved = CalculatorLayout.reservedHeightOutsideKeypad(textScale: 1)

        #expect(reserved == CalculatorLayout.reservedHeightOutsideKeypad)
        #expect(
            CalculatorLayout.keypadWidth(contentWidth: 343, height: 561, textScale: 1)
                == CalculatorLayout.keypadWidth(contentWidth: 343, height: 561),
        )
    }

    @Test("글자가 커지면 디스플레이 글자 높이만큼 키패드 밖 높이를 더 남긴다")
    func 글자가_커지면_키패드_밖_높이를_더_남긴다() {
        let reserved = CalculatorLayout.reservedHeightOutsideKeypad(textScale: 1.4)
        let expected = CalculatorLayout.reservedHeightOutsideKeypad + CalculatorLayout.displayTextHeight * 0.4

        #expect(abs(reserved - expected) < 0.001)
    }

    @Test("글자가 커지면 높이가 모자란 화면에서 키패드가 그만큼 작아진다")
    func 글자가_커지면_높이가_모자란_화면에서_키패드가_작아진다() {
        // iPhone SE 세로
        let height: CGFloat = 561
        let textScale: CGFloat = 1.4
        let width = CalculatorLayout.keypadWidth(contentWidth: 343, height: height, textScale: textScale)
        let reserved = CalculatorLayout.reservedHeightOutsideKeypad(textScale: textScale)

        #expect(width < CalculatorLayout.keypadWidth(contentWidth: 343, height: height))
        #expect(abs(width * 5 / 4 + reserved - height) < 0.001)
    }

    @Test("글자를 기본보다 작게 해도 키패드를 기본보다 키우지 않는다")
    func 글자를_작게_해도_키패드를_키우지_않는다() {
        let reserved = CalculatorLayout.reservedHeightOutsideKeypad(textScale: 0.8)

        #expect(reserved == CalculatorLayout.reservedHeightOutsideKeypad)
    }

    @Test("디스플레이 글자 비율은 기본 글자 크기에서 1이고, 계산기 상한에서 1보다 크다")
    func 디스플레이_글자_비율은_기본에서_1이다() {
        #expect(CalculatorDisplayView.textScale(for: .large) == 1)
        #expect(CalculatorDisplayView.textScale(for: CalculatorView.maxDynamicTypeSize) > 1)
    }

    @Test("환산 금액 몫을 남기지 않으면 키패드 밖 높이에서 환산 금액 두 줄 몫을 글자 비율만큼 뺀다", arguments: [1.0, 1.4])
    func 환산_금액_몫을_남기지_않으면_그만큼_뺀다(textScale: CGFloat) {
        let reserved = CalculatorLayout.reservedHeightOutsideKeypad(
            textScale: textScale,
            reservesConvertedAmounts: false,
        )
        let expected = CalculatorLayout.reservedHeightOutsideKeypad(textScale: textScale)
            - CalculatorLayout.convertedAmountsHeight * textScale

        #expect(abs(reserved - expected) < 0.001)
    }

    @Test("키 한 칸 하한을 주면 높이가 모자라도 키가 하한보다 작아지지 않는다")
    func 키_하한을_주면_높이가_모자라도_지킨다() {
        let minimumKeySize = CalculatorLayout.minimumKeySize
        // 남는 높이로 정한 폭(160)은 하한(224)보다 작지만, 디스플레이를 내주면 하한이 들어가는 높이
        let width = CalculatorLayout.keypadWidth(
            contentWidth: Self.seWidth,
            height: 400,
            minimumKeySize: minimumKeySize,
        )

        #expect(width == minimumKeySize * 4)
    }

    @Test("키 한 칸 하한은 내용 폭을 넘지 않는다")
    func 키_하한은_내용_폭을_넘지_않는다() {
        let width = CalculatorLayout.keypadWidth(
            contentWidth: 180,
            height: 400,
            minimumKeySize: CalculatorLayout.minimumKeySize,
        )

        #expect(width == 180)
    }

    @Test("디스플레이를 다 내줘도 하한이 들어가지 않는 낮은 높이에서는 키패드가 높이 안에 들어간다", arguments: [
        CGFloat(207), // 가로 화면 높이 300에서 실패 문구 93을 뺀 값
        CGFloat(120),
        CGFloat(0),
    ])
    func 낮은_높이에서는_하한보다_높이_안에_들어가는_것을_지킨다(height: CGFloat) {
        let width = CalculatorLayout.keypadWidth(
            contentWidth: 180,
            height: height,
            minimumKeySize: CalculatorLayout.minimumKeySize,
        )
        // 키패드 말고 늘 자리를 차지하는 높이 중 디스플레이 글자를 뺀 것: 통화 선택 줄·디스플레이 여백·아래 여백
        let fixedHeight = CalculatorLayout.reservedHeightOutsideKeypad - CalculatorLayout.displayTextHeight

        #expect(width >= 0)
        #expect(width < CalculatorLayout.minimumKeySize * 4)
        #expect(width * 5 / 4 + fixedHeight <= max(height, fixedHeight) + 0.001)
    }

    @Test("키가 하한보다 크면 하한은 키패드 폭을 바꾸지 않는다")
    func 키가_하한보다_크면_하한은_걸리지_않는다() {
        // iPhone SE 세로, 기본 글자 크기의 정상 상태: 키 한 칸 72pt
        let width = CalculatorLayout.keypadWidth(
            contentWidth: Self.seWidth,
            height: Self.seHeight,
            minimumKeySize: CalculatorLayout.minimumKeySize,
        )

        #expect(width == CalculatorLayout.keypadWidth(contentWidth: Self.seWidth, height: Self.seHeight))
    }

    @Test("SE 실패 상태는 계산기 글자 크기 상한에서도 키 한 칸이 하한 아래로 내려가지 않는다", arguments: [
        // 실제 실패 문구 높이(두 줄·세 줄). 이보다 훨씬 높아 하한이 화면에 들어가지 않으면 하한보다 화면 안에
        // 들어가는 것을 지킨다(`낮은_높이에서는_하한보다_높이_안에_들어가는_것을_지킨다`).
        twoLineFailureHeight, threeLineFailureHeight,
    ])
    func SE_실패_상태는_글자_크기_상한에서도_키_하한을_지킨다(failureHeight: CGFloat) {
        let width = failureKeypadWidth(failureHeight: failureHeight)

        #expect(width / 4 >= CalculatorLayout.minimumKeySize)
    }

    @Test("SE 실패 상태에서 두 줄 실패 문구면 키가 하한 이상이고, 디스플레이에 수식 한 줄과 미리보기가 보인다")
    func SE_실패_상태_두_줄_문구면_수식_한_줄과_미리보기가_보인다() {
        let failureHeight = Self.twoLineFailureHeight
        let keypadWidth = failureKeypadWidth(failureHeight: failureHeight)
        // 키패드 밖 높이 중 통화 선택 줄·디스플레이 위아래 여백·키패드 아래 여백을 뺀, 디스플레이 글자에 남는 높이
        let displayTextSpace = Self.seHeight - failureHeight - keypadWidth * 5 / 4
            - (CalculatorLayout.reservedHeightOutsideKeypad - CalculatorLayout.displayTextHeight)
        let size = CalculatorView.maxDynamicTypeSize
        // 짧은 수식일 때가 가장 큰 글자다.
        let expressionFont = UIFont.systemFont(
            ofSize: ScaledFont.size(CalculatorDisplayView.expressionFontSize(forLength: 1), for: size),
            weight: .light,
        )
        let previewFont = UIFont.systemFont(
            ofSize: ScaledFont.size(CalculatorDisplayView.previewFontSize, for: size),
            weight: .medium,
        )
        // 디스플레이는 자르고 수식 칸은 끝부분으로 스크롤하므로, 수식 줄 위쪽의 빈 공간(어센더 - 대문자 높이)은 잘려도
        // 숫자가 보인다. 기본 글자 크기의 정상 상태도 줄 높이를 다 남기지 않고 이렇게 자른다(108 < 줄 높이 합 약 120).
        let expressionVisibleHeight = expressionFont.lineHeight - (expressionFont.ascender - expressionFont.capHeight)

        #expect(keypadWidth / 4 >= CalculatorLayout.minimumKeySize)
        #expect(displayTextSpace >= expressionVisibleHeight + previewFont.lineHeight)
    }

    /// `CalculatorView`의 실패 상태와 같이 정한 iPhone SE 세로의 키패드 폭: 실패 문구 높이를 빼고, 환율이 없어
    /// 환산 금액 몫을 남기지 않고, 키 하한을 건다. 글자 크기는 계산기 상한이다.
    private func failureKeypadWidth(failureHeight: CGFloat) -> CGFloat {
        CalculatorLayout.keypadWidth(
            contentWidth: Self.seWidth,
            height: Self.seHeight - failureHeight,
            textScale: CalculatorDisplayView.textScale(for: CalculatorView.maxDynamicTypeSize),
            reservesConvertedAmounts: false,
            minimumKeySize: CalculatorLayout.minimumKeySize,
        )
    }
}
