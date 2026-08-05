import Testing
@testable import Data

@Suite("CurrencyInfoMapper")
struct CurrencyInfoMapperTests {
    @Test("지원하는 통화 코드는 국기 이모지로 변환한다")
    func 국기_이모지_변환() {
        #expect(flagEmoji(for: "KRW") == "🇰🇷")
        #expect(flagEmoji(for: "USD") == "🇺🇸")
        #expect(flagEmoji(for: "EUR") == "🇪🇺")
    }

    @Test("지원하지 않는 통화 코드는 빈 문자열을 반환한다")
    func 미지원_통화는_빈_문자열() {
        #expect(flagEmoji(for: "XXX").isEmpty)
    }

    @Test("KRW의 기준 환율은 항상 1.0이다")
    func KRW_기준_환율은_1() {
        let rates: [ExchangeRateData] = []

        #expect(rates.rate(of: "KRW") == 1.0)
        #expect(rates.rate(of: "USD") == nil)
    }
}
