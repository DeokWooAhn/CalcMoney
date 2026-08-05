import SwiftUI
import UIKit

/// 계산기 외 화면들의 공통 색상 (Android `Color.kt`의 라이트/다크 팔레트 대응)
enum AppColors {
    static let background = Color.dynamic(light: 0xFAFAFA, dark: 0x000000)
    static let surface = Color.dynamic(light: 0xF0F0F0, dark: 0x1E1E1E)
    static let currencySelectorSurface = Color.dynamic(light: 0xE9ECEF, dark: 0x24242A)
    static let currencySelectorBorder = Color.dynamic(light: 0xD0D7DE, dark: 0x3A3A42)
}

extension Color {
    /// 라이트/다크 모드에 따라 다른 16진수 색을 쓰는 동적 색상을 만든다.
    static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
        })
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1,
        )
    }
}
