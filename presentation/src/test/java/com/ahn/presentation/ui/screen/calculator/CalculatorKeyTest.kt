package com.ahn.presentation.ui.screen.calculator

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CalculatorKeyTest :
    DescribeSpec({
        describe("계산기 키를 Intent로 변환") {
            it("숫자 키는 숫자 토큰 입력으로 변환한다") {
                CalculatorKey.Number("7").toIntent() shouldBe
                    CalculatorContract.Intent.Input(CalculatorToken.Number("7"))
            }

            it("연산자 키는 화면 표시 문자가 아니라 입력값으로 변환한다") {
                CalculatorKey.Operator(displayText = "−", inputValue = "-").toIntent() shouldBe
                    CalculatorContract.Intent.Input(CalculatorToken.Operator("-"))
            }

            it("나누기 키는 입력값도 나눗셈 기호를 유지한다") {
                CalculatorKey.Operator(displayText = "÷", inputValue = "÷").toIntent() shouldBe
                    CalculatorContract.Intent.Input(CalculatorToken.Operator("÷"))
            }

            it("소수점 키는 소수점 토큰 입력으로 변환한다") {
                CalculatorKey.Dot.toIntent() shouldBe
                    CalculatorContract.Intent.Input(CalculatorToken.Dot)
            }

            it("괄호 키는 괄호 토큰 입력으로 변환한다") {
                CalculatorKey.Parenthesis.toIntent() shouldBe
                    CalculatorContract.Intent.Input(CalculatorToken.Parenthesis)
            }

            it("AC 키는 Clear로 변환한다") {
                CalculatorKey.Clear.toIntent() shouldBe CalculatorContract.Intent.Clear
            }

            it("지우기 키는 Delete로 변환한다") {
                CalculatorKey.Delete.toIntent() shouldBe CalculatorContract.Intent.Delete
            }

            it("등호 키는 Calculate로 변환한다") {
                CalculatorKey.Calculate.toIntent() shouldBe CalculatorContract.Intent.Calculate
            }

            it("기록 키는 ViewModel 의도가 아니므로 변환되지 않는다") {
                CalculatorKey.History.toIntent() shouldBe null
            }
        }

        describe("키패드 배열") {
            it("5행 4열이다") {
                calculatorKeyRows.size shouldBe 5
                calculatorKeyRows.all { it.size == 4 } shouldBe true
            }

            it("기록 키를 뺀 모든 키가 Intent로 변환된다") {
                calculatorKeyRows
                    .flatten()
                    .filterNot { it == CalculatorKey.History }
                    .any { it.toIntent() == null } shouldBe false
            }

            it("기록 키는 첫 행에 하나만 있다") {
                calculatorKeyRows.flatten().count { it == CalculatorKey.History } shouldBe 1
                calculatorKeyRows.first() shouldBe
                    listOf(
                        CalculatorKey.History,
                        CalculatorKey.Clear,
                        CalculatorKey.Parenthesis,
                        CalculatorKey.Operator(displayText = "÷", inputValue = "÷"),
                    )
            }
        }
    })
