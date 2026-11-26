import SwiftUI
import Testing
import UIKit
@testable import Presentation

@Suite("ScaledFont")
struct ScaledFontTests {
    /// 앱이 쓰는 모든 기준 크기 (FastScrollIndex 제외). `@Test` 인자는 MainActor 밖에서 읽으므로 `nonisolated`로 둔다.
    private nonisolated static let baseSizes: [CGFloat] = [
        11, 12, 13, 14, 15, 16, 17, 18, 20, 22, 24, 25, 28, 30, 31, 35, 42,
    ]

    @Test("기본 글자 크기(Large)에서는 기준 크기를 그대로 쓴다", arguments: baseSizes)
    func 기본_글자_크기에서는_기준_크기를_그대로_쓴다(size: CGFloat) {
        #expect(ScaledFont.size(size, for: .large) == size)
    }

    @Test("글자 크기를 키우면 기준 크기보다 커진다", arguments: baseSizes)
    func 글자_크기를_키우면_커진다(size: CGFloat) {
        #expect(ScaledFont.size(size, for: .xxxLarge) > size)
        #expect(ScaledFont.size(size, for: .accessibility2) > ScaledFont.size(size, for: .xxxLarge))
    }

    @Test("글자 크기를 줄이면 기준 크기보다 커지지 않는다", arguments: baseSizes)
    func 글자_크기를_줄이면_커지지_않는다(size: CGFloat) {
        // 가장 작은 caption2(11)는 시스템이 더 줄이지 않으므로 작아지는지는 본문 크기로 따로 본다.
        #expect(ScaledFont.size(size, for: .xSmall) <= size)
    }

    @Test("글자 크기를 줄이면 본문 크기 글자는 작아진다")
    func 글자_크기를_줄이면_본문_크기_글자는_작아진다() {
        #expect(ScaledFont.size(17, for: .xSmall) < 17)
    }

    @Test("글자 크기 단계를 올릴수록 크기가 줄지 않는다", arguments: baseSizes)
    func 글자_크기_단계를_올릴수록_줄지_않는다(size: CGFloat) {
        let sizes = DynamicTypeSize.allCases.map { ScaledFont.size(size, for: $0) }

        #expect(zip(sizes, sizes.dropFirst()).allSatisfy { $0 <= $1 })
    }

    @Test("같은 글자 크기에서 기준 크기가 바뀌면 결과도 그 순서대로 바뀐다")
    func 기준_크기가_바뀌면_결과도_바뀐다() {
        // 계산기 수식은 길이에 따라 42 → 35 → 30으로 기준 크기를 바꾼다.
        let expressionSizes = [42, 35, 30].map { ScaledFont.size($0, for: .xxxLarge) }

        #expect(expressionSizes[0] > expressionSizes[1])
        #expect(expressionSizes[1] > expressionSizes[2])
    }

    @Test("기준 크기는 가장 가까운 텍스트 스타일의 비율로 키운다", arguments: [
        (CGFloat(11), UIFont.TextStyle.caption2),
        (12, .caption1),
        (13, .footnote),
        (14, .subheadline),
        (15, .subheadline),
        (16, .callout),
        (17, .body),
        (19, .body),
        (20, .title3),
        (21, .title3),
        (22, .title2),
        (27, .title2),
        (28, .title1),
        (33, .title1),
        (34, .largeTitle),
        (42, .largeTitle),
    ])
    func 기준_크기에_맞는_텍스트_스타일을_고른다(size: CGFloat, style: UIFont.TextStyle) {
        #expect(ScaledFont.textStyle(forBaseSize: size) == style)
    }

    @Test("작은 글자는 큰 글자보다 더 큰 비율로 커진다")
    func 작은_글자는_더_큰_비율로_커진다() {
        let captionRatio = ScaledFont.size(12, for: .xxxLarge) / 12
        let largeTitleRatio = ScaledFont.size(42, for: .xxxLarge) / 42

        #expect(captionRatio > largeTitleRatio)
    }
}
