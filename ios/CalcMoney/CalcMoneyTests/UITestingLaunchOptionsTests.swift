import Foundation
import Testing
@testable import CalcMoney

@Suite("UI 테스트 시작 상태")
@MainActor
struct UITestingLaunchOptionsTests {
    @Test("UI 테스트 표시 없이 초기화 인자만 주면 설정을 지우지 않는다")
    func 테스트_표시가_없으면_초기화하지_않는다() {
        let options = UITestingLaunchOptions(arguments: ["UI_TESTING_RESET", "FIXED_EXCHANGE_RATES"])
        #expect(!options.resetsState)
        #expect(!options.usesFixedRates)
    }

    @Test("초기화가 비활성이면 기존 설정을 보존한다", arguments: [[], ["UI_TESTING"], ["UI_TESTING_RESET"]])
    func 비활성_초기화는_설정을_보존한다(arguments: [String]) throws {
        let prefix = "test.\(UUID().uuidString)"
        let domain = "\(prefix).currency_selection"
        let defaults = try #require(UserDefaults(suiteName: domain))
        defer { UserDefaults.standard.removePersistentDomain(forName: domain) }
        defaults.set("existing", forKey: "state")

        UITestingLaunchOptions(arguments: arguments).preparePreferences(prefix: prefix)

        #expect(defaults.string(forKey: "state") == "existing")
    }

    @Test("명시적인 초기화는 앱의 설정만 지우고 다른 도메인은 보존한다")
    func 초기화는_앱_소유_도메인만_지운다() throws {
        let prefix = "test.\(UUID().uuidString)"
        let other = "\(prefix).unrelated"
        defer {
            for name in UITestingLaunchOptions.preferenceSuites + ["unrelated"] {
                UserDefaults.standard.removePersistentDomain(forName: "\(prefix).\(name)")
            }
        }
        let suites = try UITestingLaunchOptions.preferenceSuites.map { name in
            try #require(UserDefaults(suiteName: "\(prefix).\(name)"))
        }
        let unrelated = try #require(UserDefaults(suiteName: other))
        for defaults in suites + [unrelated] { defaults.set("existing", forKey: "state") }
        let options = UITestingLaunchOptions(arguments: ["UI_TESTING", "UI_TESTING_RESET"])
        options.preparePreferences(prefix: prefix)
        for defaults in suites {
            #expect(defaults.string(forKey: "state") == nil)
        }
        #expect(unrelated.string(forKey: "state") == "existing")
        #expect(!options.usesFixedRates)
    }

    @Test("UI 테스트의 고정 환율과 초기화는 별도 인자로 선택한다")
    func 고정_환율과_초기화를_따로_선택한다() {
        let options = UITestingLaunchOptions(arguments: ["UI_TESTING", "FIXED_EXCHANGE_RATES"])
        #expect(options.usesFixedRates)
        #expect(!options.resetsState)
    }
}
