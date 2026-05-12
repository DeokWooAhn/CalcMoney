package com.ahn.domain.exchange.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CalculateExchangeAmountUseCaseTest : DescribeSpec({

    val useCase = CalculateExchangeAmountUseCase()

    describe("환전 금액 계산") {
        it("입력 금액과 환율을 곱해 소수점 둘째 자리까지 반환한다") {
            useCase(
                fromAmount = "1000",
                rate = 1500.0,
            ) shouldBe "1500000.00"
        }

        it("소수점 결과도 둘째 자리까지 반환한다") {
            useCase(
                fromAmount = "1",
                rate = 0.00072,
            ) shouldBe "0.00"
        }

        it("계산 결과가 0이면 빈 문자열을 반환한다") {
            useCase(
                fromAmount = "0",
                rate = 1500.0,
            ) shouldBe ""
        }

        it("입력 금액이 숫자가 아니면 빈 문자열을 반환한다") {
            useCase(
                fromAmount = "abc",
                rate = 1500.0,
            ) shouldBe ""
        }

        it("환율이 0이면 빈 문자열을 반환한다") {
            useCase(
                fromAmount = "1000",
                rate = 0.0,
            ) shouldBe ""
        }
    }
})