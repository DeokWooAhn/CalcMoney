import SwiftUI
import Testing
import UIKit
@testable import Presentation

@Suite("AppColors")
struct AppColorsTests {
    /// UIKit은 dynamic provider 클로저를 SwiftUI 백그라운드 렌더 스레드에서도 부른다.
    /// 클로저가 MainActor 격리로 추론되면 그 순간 런타임 격리 검사가 SIGTRAP으로 앱을 죽인다.
    /// (수정 전 코드에서는 이 테스트가 통과에 실패하는 게 아니라 테스트 프로세스가 통째로 죽는다.)
    ///
    /// Task.detached는 어떤 액터도 상속하지 않으므로 백그라운드 해석 상황을 확실하게 재현한다.
    @Test("동적 색상은 메인 액터 밖에서 해석해도 죽지 않는다")
    func 동적_색상은_메인_액터_밖에서도_해석된다() async {
        let dynamicColor = UIColor(Color.dynamic(light: 0xFAFAFA, dark: 0x000000))

        let (darkWhite, lightWhite) = await Task.detached { () -> (CGFloat, CGFloat) in
            let dark = dynamicColor.resolvedColor(
                with: UITraitCollection(userInterfaceStyle: .dark),
            )
            let light = dynamicColor.resolvedColor(
                with: UITraitCollection(userInterfaceStyle: .light),
            )

            var darkValue: CGFloat = -1
            var lightValue: CGFloat = -1
            dark.getWhite(&darkValue, alpha: nil)
            light.getWhite(&lightValue, alpha: nil)

            return (darkValue, lightValue)
        }.value

        // dark 0x000000 → 0.0, light 0xFAFAFA → 250/255
        #expect(darkWhite == 0.0)
        #expect(abs(lightWhite - 250.0 / 255.0) < 0.001)
    }
}
