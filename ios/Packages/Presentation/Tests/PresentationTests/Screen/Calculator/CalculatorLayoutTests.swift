import CoreGraphics
import Testing
@testable import Presentation

@Suite("CalculatorLayout")
struct CalculatorLayoutTests {
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
}
