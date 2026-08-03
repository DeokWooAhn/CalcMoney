import SwiftUI

/// 하단 탭 4개 정의 (Android `BottomNavItem`/`Route` 대응)
enum MainTab: String, CaseIterable, Identifiable {
    case calculator
    case exchange
    case favorite
    case setting

    var id: String { rawValue }

    var title: String {
        switch self {
        case .calculator: "계산기"
        case .exchange: "환율"
        case .favorite: "즐겨찾기"
        case .setting: "설정"
        }
    }

    var systemImage: String {
        switch self {
        case .calculator: "plus.forwardslash.minus"
        case .exchange: "arrow.left.arrow.right.circle"
        case .favorite: "star"
        case .setting: "gearshape"
        }
    }
}
