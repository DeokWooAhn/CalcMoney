import SwiftUI

extension Color {
    /// 0xRRGGBB 16진수로 색을 만든다.
    init(hex: UInt32) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255,
        )
    }
}

/// 계산기 화면 색상 팔레트.
/// 다크는 검정 배경 + 틸 강조, 라이트는 밝은 배경 + 그린 강조, C·⌫는 코랄 레드.
struct CalculatorColors {
    let background: Color
    let keyBackground: Color
    let operatorKeyBackground: Color
    let keyText: Color
    let destructive: Color
    let accent: Color
    let equalsBackground: Color
    let preview: Color
    let snackbarBackground: Color
    let hasKeyShadow: Bool

    static func palette(for colorScheme: ColorScheme) -> CalculatorColors {
        colorScheme == .dark ? .dark : .light
    }

    static let dark = CalculatorColors(
        background: .black,
        keyBackground: Color(hex: 0x2A2A2C),
        operatorKeyBackground: Color(hex: 0x2A2A2C),
        keyText: .white,
        destructive: Color(hex: 0xEF6E62),
        accent: Color(hex: 0x2FC0B0),
        equalsBackground: Color(hex: 0x199C8C),
        preview: Color(hex: 0x8E8E93),
        snackbarBackground: Color(hex: 0x48484A),
        hasKeyShadow: false,
    )

    static let light = CalculatorColors(
        background: Color(hex: 0xF6F6F6),
        keyBackground: .white,
        operatorKeyBackground: Color(hex: 0xDCDCDE),
        keyText: Color(hex: 0x1C1C1E),
        destructive: Color(hex: 0xE5695C),
        accent: Color(hex: 0x0AA968),
        equalsBackground: Color(hex: 0x0AA968),
        preview: Color(hex: 0x9A9A9E),
        snackbarBackground: Color(hex: 0x323234),
        hasKeyShadow: true,
    )
}
