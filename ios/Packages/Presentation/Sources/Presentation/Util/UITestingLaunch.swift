import Foundation

/// UI 테스트(XCUITest·Maestro)로 실행됐는지 알려 준다 (Android `MainActivity.isUiTesting` 대응)
///
/// 광고 동의 흐름과 인앱 리뷰 요청처럼 테스트 화면을 덮는 시스템 UI를 끄는 데 쓴다.
enum UITestingLaunch {
    /// UI 테스트가 넘기는 실행 인자. `CalcMoneyUITests`의 `launchApp()`, Maestro `launch_clean.yaml`과 값이 같아야 한다.
    ///
    /// UserDefaults가 하이픈으로 시작하는 인자를 키-값 쌍으로 해석해 뒤따르는 인자를 삼키므로
    /// (`-AppleLanguages (ko)`처럼) 여기서는 하이픈을 붙이지 않는다.
    static let argument = "UI_TESTING"

    static var isActive: Bool {
        ProcessInfo.processInfo.arguments.contains(argument)
    }
}
