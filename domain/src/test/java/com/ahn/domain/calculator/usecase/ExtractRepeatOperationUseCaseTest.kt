package com.ahn.domain.calculator.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExtractRepeatOperationUseCaseTest : DescribeSpec({

    val useCase = ExtractRepeatOperationUseCase()

    describe("반복 연산 추출") {
        it("덧셈 수식에서 마지막 연산과 피연산자를 추출한다") {
            useCase("2+1") shouldBe "+1"
        }

        it("뺄셈 수식에서 마지막 연산과 피연산자를 추출한다") {
            useCase("10-3") shouldBe "-3"
        }

        it("곱셈 수식에서 마지막 연산과 피연산자를 추출한다") {
            useCase("4×5") shouldBe "×5"
        }

        it("나눗셈 수식에서 마지막 연산과 피연산자를 추출한다") {
            useCase("10÷2") shouldBe "÷2"
        }

        it("음수 피연산자도 반복 연산에 포함한다") {
            useCase("2×-1") shouldBe "×-1"
        }

        it("유니코드 음수 피연산자도 반복 연산에 포함한다") {
            useCase("2×−1") shouldBe "×−1"
        }

        it("연산자가 없으면 null을 반환한다") {
            useCase("100") shouldBe null
        }

        it("연산자로 끝나는 수식이면 null을 반환한다") {
            useCase("10+") shouldBe null
        }

        it("오른쪽 피연산자가 숫자가 아니면 null을 반환한다") {
            useCase("10+abc") shouldBe null
        }

        it("첫 글자가 연산자인 단일 음수는 null을 반환한다") {
            useCase("-10") shouldBe null
        }

        it("첫 글자가 유니코드 음수인 단일 음수는 null을 반환한다") {
            useCase("−10") shouldBe null
        }
    }
})
