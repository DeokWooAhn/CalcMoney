import Testing
@testable import Domain

@Suite("CalculateExchangeAmountUseCase — 환전 금액 계산")
struct CalculateExchangeAmountUseCaseTests {
    private let useCase = CalculateExchangeAmountUseCase()

    @Test("입력 금액과 환율을 곱해 소수점 둘째 자리까지 반환한다")
    func 소수점_둘째_자리까지_반환() {
        #expect(useCase(fromAmount: "1000", rate: 1500.0) == "1500000.00")
    }

    @Test("소수점 결과도 둘째 자리까지 반환한다")
    func 소수점_결과() {
        #expect(useCase(fromAmount: "1", rate: 0.00072) == "0.00")
    }

    @Test("계산 결과가 0이면 빈 문자열을 반환한다")
    func 결과가_0() {
        #expect(useCase(fromAmount: "0", rate: 1500.0) == "")
    }

    @Test("입력 금액이 숫자가 아니면 빈 문자열을 반환한다")
    func 숫자가_아닌_입력() {
        #expect(useCase(fromAmount: "abc", rate: 1500.0) == "")
    }

    @Test("환율이 0이면 빈 문자열을 반환한다")
    func 환율이_0() {
        #expect(useCase(fromAmount: "1000", rate: 0.0) == "")
    }
}
