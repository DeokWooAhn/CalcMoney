import Domain
import Foundation
import Testing
@testable import Presentation

@Suite("통화 이름")
struct CurrencyNameTests {
    private let english = Locale(identifier: "en")
    private let korean = Locale(identifier: "ko")

    @Test("플랫폼이 아는 통화는 코드가 아닌 현지화 이름을 반환한다")
    func 플랫폼이_아는_통화는_이름을_반환한다() {
        let name = platformCurrencyName("twd", locale: Locale(identifier: "ko"))

        #expect(name != nil)
        #expect(name?.uppercased() != "TWD")
    }

    @Test("ISO 4217에 없는 지역 통화는 nil을 반환한다")
    func 플랫폼이_모르는_통화는_nil() {
        #expect(platformCurrencyName("GGP", locale: Locale(identifier: "ko")) == nil)
    }

    @Test("빈 통화 코드는 nil을 반환한다")
    func 빈_통화_코드는_nil() {
        #expect(platformCurrencyName("", locale: Locale(identifier: "ko")) == nil)
    }

    @Test("플랫폼이 모르는 지역 통화는 영어 화면에서도 서버의 한국어 이름 대신 예비 이름을 쓴다")
    func 플랫폼이_모르는_지역_통화는_예비_이름을_쓴다() {
        let jep = CurrencyInfo(code: "JEP", displayCode: "JEP", name: "저지 파운드(서버)", flagEmoji: "🇯🇪")

        let name = jep.localizedName(locale: english)

        #expect(name == LDynamic("저지 파운드"))
        #expect(name != "저지 파운드(서버)")
    }

    @Test("예비 이름이 있어도 플랫폼이 아는 통화는 플랫폼 이름을 쓴다")
    func 플랫폼이_아는_통화는_예비_이름보다_플랫폼_이름을_쓴다() {
        let mru = CurrencyInfo(code: "MRU", displayCode: "MRU", name: "모리타니 우기야(서버)", flagEmoji: "🇲🇷")
        let platformName = platformCurrencyName("MRU", locale: english)

        #expect(platformName != nil)
        #expect(mru.localizedName(locale: english) == platformName)
    }

    @Test("어디에도 이름이 없는 통화는 한국어 화면에서만 서버 이름을 쓰고 다른 언어에서는 코드를 쓴다")
    func 이름이_없는_통화는_한국어_화면에서만_서버_이름을_쓴다() {
        let unknown = CurrencyInfo(code: "QQQ", displayCode: "QQQ", name: "새 통화(서버)", flagEmoji: "🌐")

        #expect(unknown.localizedName(locale: korean) == "새 통화(서버)")
        #expect(unknown.localizedName(locale: english) == "QQQ")
    }
}
