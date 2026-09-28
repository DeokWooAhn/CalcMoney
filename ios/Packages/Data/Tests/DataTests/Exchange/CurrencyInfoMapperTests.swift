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

    @Test("국가를 특정할 수 없는 통화 코드는 중립 아이콘을 반환한다")
    func 미지원_통화는_중립_아이콘() {
        #expect(flagEmoji(for: "XXX") == "🌐")
    }

    @Test("통화 코드 앞 두 글자의 국가 국기를 사용한다")
    func 앞_두_글자로_국기를_만든다() {
        #expect(flagEmoji(for: "TWD") == "🇹🇼")
        #expect(flagEmoji(for: "VND") == "🇻🇳")
        #expect(flagEmoji(for: "CNY") == "🇨🇳")
        #expect(flagEmoji(for: "CNH") == "🇨🇳")
    }

    @Test("대소문자를 구분하지 않는다")
    func 소문자_코드도_변환한다() {
        #expect(flagEmoji(for: "php") == "🇵🇭")
    }

    @Test("앞 두 글자가 국가 코드가 아닌 통화는 예외 매핑을 사용한다")
    func 예외_매핑을_사용한다() {
        #expect(flagEmoji(for: "ANG") == "🇨🇼")
    }

    @Test("ANG의 후속 통화인 카리브 길더는 퀴라소 국기를 사용한다")
    func 카리브_길더는_퀴라소_국기() {
        #expect(flagEmoji(for: "XCG") == "🇨🇼")
    }

    @Test("여러 나라가 함께 쓰는 X로 시작하는 통화는 중립 아이콘을 사용한다", arguments: ["XAF", "XOF", "XCD", "XPF", "XDR"])
    func 공용_통화는_중립_아이콘(code: String) {
        #expect(flagEmoji(for: code) == noCountryFlag)
    }

    @Test("앞 두 글자가 국가 코드가 아니면 중립 아이콘을 사용한다")
    func 국가_코드가_아니면_중립_아이콘() {
        #expect(flagEmoji(for: "QQQ") == noCountryFlag)
        #expect(flagEmoji(for: "US") == noCountryFlag)
    }

    @Test("KRW의 기준 환율은 항상 1.0이다")
    func KRW_기준_환율은_1() {
        let rates: [ExchangeRateData] = []

        #expect(rates.rate(of: "KRW") == 1.0)
        #expect(rates.rate(of: "USD") == nil)
    }
}
