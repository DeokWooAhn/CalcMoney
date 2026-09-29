import SwiftUI

/// 하단 탭 4개 정의 (Android `BottomNavItem`/`Route` 대응)
enum MainTab: String, CaseIterable, Identifiable {
    case calculator
    case exchange
    case favorite
    case setting

    var id: String { rawValue }

    /// E2E 테스트(Maestro)가 탭을 찾을 때 쓰는 id. Android `BottomNavItem.testTag`와 값이 같아야 한다.
    var testID: String { "tab.\(rawValue)" }

    /// 탭 화면이 떴는지 확인하는 기준점 id. Android 각 Screen 루트의 testTag와 값이 같아야 한다.
    var screenTestID: String { "screen.\(rawValue)" }

    var title: String {
        switch self {
        case .calculator: L("계산기")
        case .exchange: L("환율")
        case .favorite: L("즐겨찾기")
        case .setting: L("설정")
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
