import Foundation

/// 테스트 실행 인자로만 상태를 초기화한다. 릴리스 빌드에서는 모든 옵션을 무시한다.
struct UITestingLaunchOptions {
    let resetsState: Bool
    let usesFixedRates: Bool

    init(arguments: [String]) {
        #if DEBUG
            let isTesting = arguments.contains("UI_TESTING")
            resetsState = isTesting && arguments.contains("UI_TESTING_RESET")
            usesFixedRates = isTesting && arguments.contains("FIXED_EXCHANGE_RATES")
        #else
            resetsState = false
            usesFixedRates = false
        #endif
    }

    /// 저장소를 만들기 전에 이 앱이 소유하는 설정만 비운다. 앱 제거·재설치는 하지 않는다.
    func preparePreferences(prefix: String = "com.deokwooahn.calcmoney") {
        guard resetsState else { return }
        for name in Self.preferenceSuites {
            UserDefaults.standard.removePersistentDomain(forName: "\(prefix).\(name)")
        }
    }

    static let preferenceSuites = [
        "currency_selection", "favorite_currencies", "theme_preferences",
        "calculator_history", "review_prompt", "interstitial_ad",
    ]
}
