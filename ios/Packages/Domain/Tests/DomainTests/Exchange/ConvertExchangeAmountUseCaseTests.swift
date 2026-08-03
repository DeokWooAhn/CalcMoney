import Testing
@testable import Domain

/// Android 테스트는 CalculateExpressionUseCase를 MockK로 대체하지만,
/// 이 유스케이스는 순수 함수라 실제 구현을 그대로 사용해 검증한다.
@Suite("ConvertExchangeAmountUseCase")
struct ConvertExchangeAmountUseCaseTests {
    private let useCase = ConvertExchangeAmountUseCase(
        calculateExpression: CalculateExpressionUseCase(),
    )

    @Suite("계산기 수식 환율 변환")
    struct 수식변환 {
        private let useCase = ConvertExchangeAmountUseCase(
            calculateExpression: CalculateExpressionUseCase(),
        )

        @Test("수식 안의 숫자를 환율로 변환하고 연산자는 유지한다")
        func 숫자_변환_연산자_유지() {
            #expect(useCase.convertExpression("100+200", rate: 0.01, currencyCode: "USD") == "1.00 USD + 2.00 USD")
        }

        @Test("소수점 숫자도 변환한다")
        func 소수점_숫자_변환() {
            #expect(useCase.convertExpression("1.5+2.5", rate: 10.0, currencyCode: "VND") == "15.00 VND + 25.00 VND")
        }

        @Test("빈 수식이면 빈 문자열을 반환한다")
        func 빈_수식() {
            #expect(useCase.convertExpression("", rate: 10.0, currencyCode: "USD") == "")
        }

        @Test("환율이 0 이하이면 빈 문자열을 반환한다")
        func 환율_0() {
            #expect(useCase.convertExpression("100+200", rate: 0.0, currencyCode: "USD") == "")
        }

        @Test("환율이 음수면 빈 문자열을 반환한다")
        func 환율_음수() {
            #expect(useCase.convertExpression("100+200", rate: -1.0, currencyCode: "USD") == "")
        }

        @Test("통화 코드가 없으면 빈 문자열을 반환한다")
        func 통화_코드_없음() {
            #expect(useCase.convertExpression("100+200", rate: 0.01, currencyCode: nil) == "")
        }

        @Test("환산 결과가 무한대면 원본 숫자를 유지한다")
        func 무한대_결과() {
            #expect(useCase.convertExpression("100+200", rate: .infinity, currencyCode: "USD") == "100 + 200")
        }
    }

    @Suite("계산기 단일 금액 환율 변환")
    struct 단일금액변환 {
        private let useCase = ConvertExchangeAmountUseCase(
            calculateExpression: CalculateExpressionUseCase(),
        )

        @Test("숫자 텍스트를 환율로 변환한다")
        func 숫자_텍스트_변환() {
            #expect(useCase.convertSingleAmount("100", rate: 0.01, currencyCode: "USD") == "1.00 USD")
        }

        @Test("달러 환산 결과의 소수점을 유지한다")
        func 달러_환산_소수점_유지() {
            #expect(useCase.convertSingleAmount("1300", rate: 1 / 1441.10, currencyCode: "USD") == "0.90 USD")
        }

        @Test("계산식 텍스트면 계산 결과를 환율로 변환한다")
        func 계산식_텍스트_변환() {
            #expect(useCase.convertSingleAmount("100+200", rate: 0.01, currencyCode: "USD") == "3.00 USD")
        }

        @Test("계산식 결과가 Error이면 빈 문자열을 반환한다")
        func 계산식_결과_Error() {
            #expect(useCase.convertSingleAmount("1++", rate: 0.01, currencyCode: "USD") == "")
        }

        @Test("빈 텍스트면 빈 문자열을 반환한다")
        func 빈_텍스트() {
            #expect(useCase.convertSingleAmount("", rate: 0.01, currencyCode: "USD") == "")
        }

        @Test("환산 결과가 유한하지 않으면 빈 문자열을 반환한다")
        func 환산_결과_무한대() {
            #expect(useCase.convertSingleAmount("100", rate: .infinity, currencyCode: "USD") == "")
        }

        @Test("입력 금액이 유한하지 않으면 빈 문자열을 반환한다")
        func 입력_금액_무한대() {
            #expect(useCase.convertSingleAmount("Infinity", rate: 1.0, currencyCode: "USD") == "")
        }
    }
}
