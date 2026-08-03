import Testing
@testable import Domain

@Suite("BuildFavoriteRatesUseCase — 즐겨찾기 환율 목록 생성")
struct BuildFavoriteRatesUseCaseTests {
    private let useCase = BuildFavoriteRatesUseCase()

    private let krw = CurrencyInfo(code: "KRW", displayCode: "KRW", name: "대한민국 원", flagEmoji: "🇰🇷")
    private let usd = CurrencyInfo(code: "USD", displayCode: "USD", name: "미국 달러", flagEmoji: "🇺🇸")
    private let jpy = CurrencyInfo(code: "JPY", displayCode: "JPY", name: "일본 엔", flagEmoji: "🇯🇵")

    @Test("즐겨찾기 통화와 환율 정보를 이용해 변환 금액을 계산한다")
    func 변환_금액_계산() {
        let result = useCase(
            baseCurrency: krw,
            baseAmount: "1000",
            favoriteCurrencyCodes: ["USD", "JPY"],
            availableCurrencies: [krw, usd, jpy],
            ratesByCode: ["USD": 0.00072, "JPY": 0.11],
        )

        #expect(result.map(\.currency.code) == ["USD", "JPY"])
        #expect(result[0].baseCurrencyCode == "KRW")
        #expect(abs(result[0].rate - 0.00072) < 0.000001)
        #expect(abs(result[0].convertedAmount - 0.72) < 0.000001)
        #expect(abs(result[1].convertedAmount - 110.0) < 0.000001)
    }

    @Test("기준 통화와 같은 즐겨찾기는 목록에서 제외한다")
    func 기준_통화_제외() {
        let result = useCase(
            baseCurrency: krw,
            baseAmount: "1000",
            favoriteCurrencyCodes: ["KRW", "USD"],
            availableCurrencies: [krw, usd],
            ratesByCode: ["USD": 0.00072],
        )

        #expect(result.map(\.currency.code) == ["USD"])
    }

    @Test("통화 정보가 없거나 환율이 없는 즐겨찾기는 제외한다")
    func 정보_없는_통화_제외() {
        let result = useCase(
            baseCurrency: krw,
            baseAmount: "1000",
            favoriteCurrencyCodes: ["USD", "JPY", "VND"],
            availableCurrencies: [krw, usd, jpy],
            ratesByCode: ["USD": 0.00072],
        )

        #expect(result.map(\.currency.code) == ["USD"])
    }

    @Test("기준 금액이 숫자가 아니면 0으로 계산한다")
    func 숫자가_아닌_기준_금액() {
        let result = useCase(
            baseCurrency: krw,
            baseAmount: "",
            favoriteCurrencyCodes: ["USD"],
            availableCurrencies: [krw, usd],
            ratesByCode: ["USD": 0.00072],
        )

        #expect(result.first?.convertedAmount == 0.0)
    }

    @Test("기준 통화가 없으면 빈 목록을 반환한다")
    func 기준_통화_없음() {
        let result = useCase(
            baseCurrency: nil,
            baseAmount: "1000",
            favoriteCurrencyCodes: ["USD"],
            availableCurrencies: [usd],
            ratesByCode: ["USD": 0.00072],
        )

        #expect(result.isEmpty)
    }
}
