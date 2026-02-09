package com.ahn.presentation.ui.screen.calculator

import com.ahn.domain.usecase.CalculatorEngine
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class CalculatorViewModelTest : BehaviorSpec({

    // MockK로 domain 의존성 Mock
    val calculatorEngine = mockk<CalculatorEngine>()

    beforeEach {
        every { calculatorEngine.calculate(any()) } returns "0"
    }

    Given("계산기 초기 상태에서") {
        When("5를 입력하면") {
            val viewModel = CalculatorViewModel(calculatorEngine)
            viewModel.processIntent(
                CalculatorContract.Intent.Input(CalculatorToken.Number("5"))
            )
            Then("expression은 '5'이어야 한다") {
                viewModel.state.value.expression shouldBe "5"
            }

            Then("커서 위치는 1이어야 한다") {
                viewModel.state.value.cursorPosition shouldBe 1
            }
        }

        When("0을 입력하면") {
            val viewModel = CalculatorViewModel(calculatorEngine)
            viewModel.processIntent(
                CalculatorContract.Intent.Input(CalculatorToken.Number("0"))
            )

            Then("expression은 '0'이어야 한다") {
                viewModel.state.value.expression shouldBe "0"
            }
        }

        When("연산자를 먼저 입력하면") {
            val viewModel = CalculatorViewModel(calculatorEngine)
            viewModel.processIntent(
                CalculatorContract.Intent.Input(CalculatorToken.Operator("+"))
            )

            Then("무시되어 expression은 비어 있어야 한다") {
                viewModel.state.value.expression shouldBe ""
            }
        }
    }

    Given("'123'이 입력된 상태에서") {

        When("AC를 누르면") {
            val viewModel = CalculatorViewModel(calculatorEngine)
            listOf("1", "2", "3").forEach {
                viewModel.processIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Number(it))
                )
            }
            viewModel.processIntent(CalculatorContract.Intent.Clear)

            Then("expression은 비어있어야 한다") {
                viewModel.state.value.expression shouldBe ""
            }

            Then("커서 위치는 0이어야 한다") {
                viewModel.state.value.cursorPosition shouldBe 0
            }
        }

        When("삭제(⌫)를 누르면") {
            val viewModel = CalculatorViewModel(calculatorEngine)
            listOf("1", "2", "3").forEach {
                viewModel.processIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Number(it))
                )
            }
            viewModel.processIntent(CalculatorContract.Intent.Delete)

            Then("마지막 글자가 지워져 '12'가 되어야 한다") {
                viewModel.state.value.expression shouldBe "12"
            }
        }
    }

    Given("'10+20'이 입력된 상태에서") {

        When("=을 누르면") {
            every { calculatorEngine.calculate("10+20") } returns "30"

            val viewModel = CalculatorViewModel(calculatorEngine)
            "10+20".forEach { ch ->
                val token = when {
                    ch.isDigit() -> CalculatorToken.Number(ch.toString())
                    else -> CalculatorToken.Operator(ch.toString())
                }
                viewModel.processIntent(CalculatorContract.Intent.Input(token))
            }
            viewModel.processIntent(CalculatorContract.Intent.Calculate)

            Then("결과 '30'이 expression에 표시되어야 한다") {
                viewModel.state.value.expression shouldBe "30"
            }
        }
    }
})