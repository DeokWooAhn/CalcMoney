import Foundation
import Testing
@testable import Presentation

@Suite("통화 이름")
struct CurrencyNameTests {
    @Test("플랫폼이 아는 통화는 코드가 아닌 현지화 이름을 반환한다")
    func 플랫폼이_아는_통화는_이름을_반환한다() {
        let name = platformCurrencyName("twd", locale: Locale(identifier: "ko"))

        #expect(name != nil)
        #expect(name?.uppercased() != "TWD")
    }

    @Test("ISO 4217에 없는 지역 통화는 서버 이름을 쓰도록 nil을 반환한다")
    func 플랫폼이_모르는_통화는_nil() {
        #expect(platformCurrencyName("GGP", locale: Locale(identifier: "ko")) == nil)
    }

    @Test("빈 통화 코드는 nil을 반환한다")
    func 빈_통화_코드는_nil() {
        #expect(platformCurrencyName("", locale: Locale(identifier: "ko")) == nil)
    }
}
