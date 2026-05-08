package com.ahn.domain.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ConvertExchangeAmountUseCaseTest : DescribeSpec({

    val calculateExpressionUseCase = mockk<CalculateExpressionUseCase>()
    val useCase = ConvertExchangeAmountUseCase(calculateExpressionUseCase)

    describe("계산기 수식 환율 변환") {
        it("수식 안의 숫자를 환율로 변환하고 연산자는 유지한다") {
            useCase.convertExpression(
                expression = "100+200",
                rate = 0.01,
                currencyCode = "USD",
            ) shouldBe "1 USD + 2 USD"
        }

        it("소수점 숫자도 변환한다") {
            useCase.convertExpression(
                expression = "1.5+2.5",
                rate = 10.0,
                currencyCode = "VND",
            ) shouldBe "15 VND + 25 VND"
        }

        it("빈 수식이면 빈 문자열을 반환한다") {
            useCase.convertExpression(
                expression = "",
                rate = 10.0,
                currencyCode = "USD",
            ) shouldBe ""
        }

        it("환율이 0 이하이면 빈 문자열을 반환한다") {
            useCase.convertExpression(
                expression = "100+200",
                rate = 0.0,
                currencyCode = "USD",
            ) shouldBe ""
        }

        it("환율이 음수면 빈 문자열을 반환한다") {
            useCase.convertExpression(
                expression = "100+200",
                rate = -1.0,
                currencyCode = "USD",
            ) shouldBe ""
        }

        it("통화 코드가 없으면 빈 문자열을 반환한다") {
            useCase.convertExpression(
                expression = "100+200",
                rate = 0.01,
                currencyCode = null,
            ) shouldBe ""
        }
    }

    describe("계산기 단일 금액 환율 변환") {
        it("숫자 텍스트를 환율로 변환한다") {
            useCase.convertSingleAmount(
                text = "100",
                rate = 0.01,
                currencyCode = "USD",
            ) shouldBe "1 USD"
        }

        it("계산식 텍스트면 계산 결과를 환율로 변환한다") {
            every { calculateExpressionUseCase.calculate("100+200") } returns "300"

            useCase.convertSingleAmount(
                text = "100+200",
                rate = 0.01,
                currencyCode = "USD",
            ) shouldBe "3 USD"
        }

        it("계산식 결과가 Error이면 빈 문자열을 반환한다") {
            every { calculateExpressionUseCase.calculate("1++") } returns "Error"

            useCase.convertSingleAmount(
                text = "1++",
                rate = 0.01,
                currencyCode = "USD",
            ) shouldBe ""
        }

        it("빈 텍스트면 빈 문자열을 반환한다") {
            useCase.convertSingleAmount(
                text = "",
                rate = 0.01,
                currencyCode = "USD",
            ) shouldBe ""
        }
    }
})