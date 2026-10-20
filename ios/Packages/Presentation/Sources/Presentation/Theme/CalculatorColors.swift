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

/// 계산기 키를 누를 때 번지는 빛과 바탕 물듦 (Android `CalculatorKeySurface`의 상수 대응)
struct CalculatorKeyPress {
    /// 원 둘레 빛의 색. 오른쪽·아래는 강조색, 왼쪽·위는 옅은 연두로 이어진다.
    let glowColors: [Color]
    let glowOpacity: Double
    let tint: Color
    /// 누른 동안 바탕을 강조색 쪽으로 물들이는 정도
    let tintAmount: Double

    /// AngularGradient는 오른쪽(3시)에서 시계 방향으로 돈다. 오른쪽·아래 → 왼쪽·위 → 다시 오른쪽 순서로 이어 붙인다.
    var glowGradient: [Color] {
        guard let accent = glowColors.first, let soft = glowColors.last else { return [] }
        return [accent, accent, soft, soft, accent]
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
    let keyPress: CalculatorKeyPress

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
        keyPress: CalculatorKeyPress(
            glowColors: [Color(hex: 0x2FC0B0), Color(hex: 0xB4E3A1)],
            glowOpacity: 0.9,
            tint: Color(hex: 0x2FC0B0),
            tintAmount: 0.15,
        ),
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
        // 밝은 배경에서는 같은 빛도 강해 보여서 다크보다 옅게 둔다.
        keyPress: CalculatorKeyPress(
            glowColors: [Color(hex: 0x0AA968), Color(hex: 0xA6E3A1)],
            glowOpacity: 0.85,
            tint: Color(hex: 0x0AA968),
            tintAmount: 0.08,
        ),
    )
}
