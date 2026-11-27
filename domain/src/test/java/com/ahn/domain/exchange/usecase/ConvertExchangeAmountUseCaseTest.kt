package com.ahn.domain.exchange.usecase

import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class ConvertExchangeAmountUseCaseTest :
    DescribeSpec({

        val calculateExpressionUseCase = mockk<CalculateExpressionUseCase>()
        val useCase = ConvertExchangeAmountUseCase(calculateExpressionUseCase)

        describe("계산기 수식 환율 변환") {
            it("수식 안의 숫자를 환율로 변환하고 연산자는 유지한다") {
                useCase.convertExpression(
                    expression = "100+200",
                    rate = 0.01,
                    currencyCode = "USD",
                ) shouldBe "1.00 USD + 2.00 USD"
            }

            it("소수점 숫자도 변환한다") {
                useCase.convertExpression(
                    expression = "1.5+2.5",
                    rate = 10.0,
                    currencyCode = "VND",
                ) shouldBe "15.00 VND + 25.00 VND"
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

            it("지수 표기 계산 결과는 지수부까지 한 숫자로 환산한다") {
                useCase.convertExpression(
                    expression = "1.2193263111E+17",
                    rate = 1.0,
                    currencyCode = "USD",
                ) shouldBe "121932631110000000.00 USD"
            }

            it("음수 지수도 한 숫자로 환산한다") {
                useCase.convertExpression(
                    expression = "2.5000000000E-11",
                    rate = 1e12,
                    currencyCode = "USD",
                ) shouldBe "25.00 USD"
            }

            it("지수 표기 결과 뒤에 이어 쓴 연산은 연산자로 나눈다") {
                useCase.convertExpression(
                    expression = "1.2193263111E+17+5",
                    rate = 2.0,
                    currencyCode = "USD",
                ) shouldBe "243865262220000000.00 USD + 10.00 USD"
            }

            it("E 뒤에 지수 숫자가 없으면 E를 숫자에 붙이지 않는다") {
                useCase.convertExpression(
                    expression = "5E+",
                    rate = 1.0,
                    currencyCode = "USD",
                ) shouldBe "5.00 USD E +"
            }

            it("환산 결과가 무한대면 원본 숫자를 유지한다") {
                useCase.convertExpression(
                    expression = "100+200",
                    rate = Double.POSITIVE_INFINITY,
                    currencyCode = "USD",
                ) shouldBe "100 + 200"
            }
        }

        describe("계산기 단일 금액 환율 변환") {
            it("숫자 텍스트를 환율로 변환한다") {
                useCase.convertSingleAmount(
                    text = "100",
                    rate = 0.01,
                    currencyCode = "USD",
                ) shouldBe "1.00 USD"
            }

            it("달러 환산 결과의 소수점을 유지한다") {
                useCase.convertSingleAmount(
                    text = "1300",
                    rate = 1 / 1441.10,
                    currencyCode = "USD",
                ) shouldBe "0.90 USD"
            }

            it("계산식 텍스트면 계산 결과를 환율로 변환한다") {
                every { calculateExpressionUseCase("100+200") } returns "300"

                useCase.convertSingleAmount(
                    text = "100+200",
                    rate = 0.01,
                    currencyCode = "USD",
                ) shouldBe "3.00 USD"
            }

            it("계산식 결과가 Error이면 빈 문자열을 반환한다") {
                every { calculateExpressionUseCase("1++") } returns "Error"

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

            it("환산 결과가 유한하지 않으면 빈 문자열을 반환한다") {
                useCase.convertSingleAmount(
                    text = "100",
                    rate = Double.POSITIVE_INFINITY,
                    currencyCode = "USD",
                ) shouldBe ""
            }

            it("입력 금액이 유한하지 않으면 빈 문자열을 반환한다") {
                useCase.convertSingleAmount(
                    text = "Infinity",
                    rate = 1.0,
                    currencyCode = "USD",
                ) shouldBe ""
            }
        }
    })
