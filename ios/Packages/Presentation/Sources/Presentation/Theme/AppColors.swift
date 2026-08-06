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
    ///
    /// UIKit은 이 dynamic provider 클로저를 SwiftUI의 백그라운드 렌더 스레드
    /// (com.apple.SwiftUI.AsyncRenderer)에서도 부른다. 패키지 기본 격리가 MainActor라
    /// nonisolated 없이 두면 클로저가 MainActor 격리로 추론되고, 백그라운드에서 불리는 순간
    /// 런타임 격리 검사(dispatch_assert_queue)가 SIGTRAP으로 앱을 죽인다.
    /// CI에서 간헐적으로 앱이 사라지던 원인이 이것이었다 (크래시 리포트로 확인).
    nonisolated static func dynamic(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            traits.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light)
        })
    }
}

private extension UIColor {
    convenience nonisolated init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1,
        )
    }
}
