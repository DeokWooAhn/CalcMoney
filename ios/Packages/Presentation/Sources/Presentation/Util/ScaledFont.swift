import SwiftUI
import UIKit

/// 시스템 글자 크기(Dynamic Type)를 따르는 고정 기준 크기 글꼴 (Android `sp` 대응)
///
/// 기본 글자 크기(Large)에서는 `.system(size:)`와 같은 크기를 내고, 글자 크기 설정에 따라 기준 크기와 가장 가까운
/// 텍스트 스타일의 비율로 커지고 작아진다. 크기는 환경의 `dynamicTypeSize`로 정하므로, 상위 뷰의
/// `.dynamicTypeSize(...)` 상한이 걸리면 그 상한에서 멈춘다.
///
/// 같은 뷰에서 기준값이 바뀌는 곳(계산기 수식의 42/35/30)도 바로 따라가도록 `@ScaledMetric` 대신
/// 기준값과 환경 값으로 body마다 계산한다.
enum ScaledFont {
    /// 기준 크기에 가장 가까운 텍스트 스타일. 이 스타일의 비율로 크기를 키운다.
    static func textStyle(forBaseSize size: CGFloat) -> UIFont.TextStyle {
        switch size {
        case ..<12: .caption2
        case ..<13: .caption1
        case ..<14: .footnote
        case ..<16: .subheadline
        case ..<17: .callout
        case ..<20: .body
        case ..<22: .title3
        case ..<28: .title2
        case ..<34: .title1
        default: .largeTitle
        }
    }

    /// 글자 크기 설정에 맞춘 크기. 기본 글자 크기(Large)에서는 `size` 그대로다.
    static func size(_ size: CGFloat, for dynamicTypeSize: DynamicTypeSize) -> CGFloat {
        let traits = UITraitCollection(preferredContentSizeCategory: UIContentSizeCategory(dynamicTypeSize))

        return UIFontMetrics(forTextStyle: textStyle(forBaseSize: size))
            .scaledValue(for: size, compatibleWith: traits)
    }
}

private struct ScaledFontModifier: ViewModifier {
    let size: CGFloat
    let weight: Font.Weight?
    let design: Font.Design?

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    func body(content: Content) -> some View {
        content.font(.system(size: ScaledFont.size(size, for: dynamicTypeSize), weight: weight, design: design))
    }
}

extension View {
    /// `.font(.system(size:weight:design:))` 대신 쓴다. 기본 글자 크기에서는 같은 크기이고, 시스템 글자 크기를 따른다.
    func scaledFont(size: CGFloat, weight: Font.Weight? = nil, design: Font.Design? = nil) -> some View {
        modifier(ScaledFontModifier(size: size, weight: weight, design: design))
    }
}
