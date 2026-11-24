import Domain
import Testing
@testable import Presentation

@Suite("resolveCalculatorCurrencies — 계산기 기준·보조 통화 결정")
struct CalculatorCurrencyResolverTests {
    private static let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "원", flagEmoji: "🇰🇷")
    private static let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "달러", flagEmoji: "🇺🇸")
    private static let eur = CurrencyInfo(code: "EUR", displayCode: "EUR", name: "유로", flagEmoji: "🇪🇺")
    private static let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "엔", flagEmoji: "🇯🇵")

    private static let all = [krw, usd, eur, jpy]

    private static func resolve(
        currencies: [CurrencyInfo] = all,
        currentMain: CurrencyInfo? = nil,
        currentSub: CurrencyInfo? = nil,
        savedSelection: CalculatorCurrencySelection? = nil,
        deviceCurrencyCode: String = "GBP",
    ) -> (main: CurrencyInfo?, sub: CurrencyInfo?) {
        resolveCalculatorCurrencies(
            currencies: currencies,
            currentMain: currentMain,
            currentSub: currentSub,
            savedSelection: savedSelection,
            deviceCurrencyCode: deviceCurrencyCode,
        )
    }

    @Test("화면에 있던 통화가 목록에 있으면 저장된 선택보다 먼저 쓴다")
    func 화면에_있던_통화가_저장된_선택보다_먼저다() {
        let result = Self.resolve(
            currentMain: Self.eur,
            currentSub: Self.jpy,
            savedSelection: CalculatorCurrencySelection(mainCode: "KRW", subCode: "USD"),
            deviceCurrencyCode: "USD",
        )

        #expect(result.main == Self.eur)
        #expect(result.sub == Self.jpy)
    }

    @Test("화면에 있던 통화가 목록에 없으면 저장된 선택을 쓴다")
    func 목록에_없는_화면_통화는_저장된_선택으로_바꾼다() {
        let missing = CurrencyInfo(code: "XXX", displayCode: "XXX", name: "없음", flagEmoji: "")

        let result = Self.resolve(
            currentMain: missing,
            currentSub: missing,
            savedSelection: CalculatorCurrencySelection(mainCode: "EUR", subCode: "JPY"),
        )

        #expect(result.main == Self.eur)
        #expect(result.sub == Self.jpy)
    }

    @Test("저장된 선택이 없으면 기준 통화는 기기 지역 통화를 쓴다")
    func 저장된_선택이_없으면_기기_통화를_쓴다() {
        let result = Self.resolve(deviceCurrencyCode: "JPY")

        #expect(result.main == Self.jpy)
    }

    @Test("기기 지역 통화가 목록에 없으면 기준 통화는 원화를 쓴다")
    func 기기_통화가_없으면_원화를_쓴다() {
        let result = Self.resolve(currencies: [Self.usd, Self.eur, Self.krw], deviceCurrencyCode: "GBP")

        #expect(result.main == Self.krw)
    }

    @Test("원화도 목록에 없으면 기준 통화는 목록 첫 통화를 쓴다")
    func 원화도_없으면_목록_첫_통화를_쓴다() {
        let result = Self.resolve(currencies: [Self.eur, Self.jpy], deviceCurrencyCode: "GBP")

        #expect(result.main == Self.eur)
    }

    @Test("저장된 선택이 없으면 보조 통화는 미국 달러를 쓴다")
    func 저장된_선택이_없으면_보조_통화는_달러다() {
        let result = Self.resolve(deviceCurrencyCode: "KRW")

        #expect(result.main == Self.krw)
        #expect(result.sub == Self.usd)
    }

    @Test("화면에 있던 보조 통화가 기준 통화와 같으면 저장된 보조 통화로 넘어간다")
    func 기준_통화와_같은_화면_보조_통화는_쓰지_않는다() {
        let result = Self.resolve(
            currentMain: Self.usd,
            currentSub: Self.usd,
            savedSelection: CalculatorCurrencySelection(mainCode: nil, subCode: "EUR"),
        )

        #expect(result.main == Self.usd)
        #expect(result.sub == Self.eur)
    }

    @Test("저장된 보조 통화가 기준 통화와 같고 기준 통화가 달러면 목록에서 기준 통화가 아닌 첫 통화를 쓴다")
    func 겹치는_후보를_모두_건너뛰면_기준_통화가_아닌_첫_통화다() {
        let result = Self.resolve(
            savedSelection: CalculatorCurrencySelection(mainCode: "USD", subCode: "USD"),
        )

        #expect(result.main == Self.usd)
        #expect(result.sub == Self.krw)
    }

    @Test("목록에 통화가 하나뿐이면 보조 통화는 정하지 않는다")
    func 통화가_하나뿐이면_보조_통화는_없다() {
        let result = Self.resolve(currencies: [Self.krw], deviceCurrencyCode: "KRW")

        #expect(result.main == Self.krw)
        #expect(result.sub == nil)
    }

    @Test("통화 목록이 비어 있으면 두 통화 모두 정하지 않는다")
    func 빈_목록이면_두_통화_모두_없다() {
        let result = Self.resolve(currencies: [])

        #expect(result.main == nil)
        #expect(result.sub == nil)
    }
}
