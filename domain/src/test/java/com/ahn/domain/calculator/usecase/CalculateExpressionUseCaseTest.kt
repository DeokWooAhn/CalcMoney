package com.ahn.domain.calculator.usecase

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CalculateExpressionUseCaseTest :
    DescribeSpec({
        val engine = CalculateExpressionUseCase()

        describe("기본 사칙연산") {
            it("덧셈: 1+2 = 3") {
                engine.calculate("1+2") shouldBe "3"
            }

            it("뺄셈: 10-3 = 7") {
                engine.calculate("10-3") shouldBe "7"
            }

            it("곱셈: 4×5 = 20") {
                engine.calculate("4×5") shouldBe "20"
            }

            it("나눗셈: 10÷2 = 5") {
                engine.calculate("10÷2") shouldBe "5"
            }
        }

        describe("소수점 계산") {
            it("1.5+2.5 = 4") {
                engine.calculate("1.5+2.5") shouldBe "4"
            }

            it("10÷3은 소수점을 포함해야 한다") {
                engine.calculate("10÷3") shouldBe "3.3333333333"
            }
        }

        describe("괄호 연산") {
            it("(1+2)×3 = 9") {
                engine.calculate("(1+2)×3") shouldBe "9"
            }

            it("중첩 괄호: ((2+3))×2 = 10") {
                engine.calculate("((2+3))×2") shouldBe "10"
            }
        }

        describe("엣지 케이스") {
            it("빈 문자열은 0을 반환한다") {
                engine.calculate("") shouldBe "0"
            }

            it("마지막이 연산자면 무시한다") {
                engine.calculate("10+") shouldBe "10"
            }

            it("잘못된 수식은 Error를 반환한다") {
                engine.calculate("++") shouldBe "Error"
            }

            it("매우 큰 수는 E 표기법을 사용한다") {
                engine.calculate("999999999999999×10") shouldBe "1.0000000000E+16"
            }
        }

        describe("E 표기법 결과를 다시 계산할 때") {
            it("큰 수 결과에 계속 연산할 수 있다") {
                engine.calculate("1.0000000000E+16×10") shouldBe "1.0000000000E+17"
                engine.calculate("1.0000000000E+16÷1.0000000000E+15") shouldBe "10"
            }

            it("음수 지수도 숫자로 읽는다") {
                engine.calculate("1.5000000000E-11×1000000000000") shouldBe "15"
            }

            it("지수부 숫자가 없으면 Error를 반환한다") {
                engine.calculate("1.0000000000E+") shouldBe "Error"
            }
        }

        describe("유한한 수가 아닌 결과") {
            it("0으로 나누면 Error를 반환한다") {
                engine.calculate("1÷0") shouldBe "Error"
                engine.calculate("0÷0") shouldBe "Error"
            }

            it("Double 범위를 넘으면 Error를 반환한다") {
                engine.calculate("1.0000000000E+308×10") shouldBe "Error"
            }
        }
    })
