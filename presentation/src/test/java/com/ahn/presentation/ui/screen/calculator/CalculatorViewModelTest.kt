package com.ahn.presentation.ui.screen.calculator

import com.ahn.domain.usecase.CalculatorEngine
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val testDispatcher = UnconfinedTestDispatcher()
    val calculatorEngine = mockk<CalculatorEngine>()
    val viewModel by lazy { CalculatorViewModel(calculatorEngine) }

    beforeEach { Dispatchers.setMain(testDispatcher) }
    afterEach { Dispatchers.resetMain() }

    Given("계산기 초기 상태에서") {
        every { calculatorEngine.calculate(any()) } returns "0"

        When("5를 입력하면") {
            viewModel.processIntent(
                CalculatorContract.Intent.Input(CalculatorToken.Number("5"))
            )

            Then("expression은 '5'이어야 한다") {
                viewModel.container.stateFlow.value.expression shouldBe "5"
            }

            Then("커서 위치는 1이어야 한다") {
                viewModel.container.stateFlow.value.cursorPosition shouldBe 1
            }
        }

        When("0을 입력하면") {
            viewModel.processIntent(
                CalculatorContract.Intent.Input(CalculatorToken.Number("0"))
            )

            Then("expression은 '0'이어야 한다") {
                viewModel.container.stateFlow.value.expression shouldBe "0"
            }
        }

        When("연산자를 먼저 입력하면") {
            viewModel.processIntent(
                CalculatorContract.Intent.Input(CalculatorToken.Operator("+"))
            )

            Then("무시되어 expression은 비어 있어야 한다") {
                viewModel.container.stateFlow.value.expression shouldBe ""
            }
        }
    }

    Given("'123'이 입력된 상태에서") {

        When("AC를 누르면") {
                listOf("1", "2", "3").forEach {
                    viewModel.processIntent(
                        CalculatorContract.Intent.Input(CalculatorToken.Number(it))
                    )
                }
            viewModel.processIntent(CalculatorContract.Intent.Clear)

            Then("expression은 비어있어야 한다") {
                viewModel.container.stateFlow.value.expression shouldBe ""
            }

            Then("커서 위치는 0이어야 한다") {
                viewModel.container.stateFlow.value.cursorPosition shouldBe 0
            }
        }

        When("삭제(⌫)를 누르면") {
            listOf("1", "2", "3").forEach {
                viewModel.processIntent(
                    CalculatorContract.Intent.Input(CalculatorToken.Number(it))
                )
            }
            viewModel.processIntent(CalculatorContract.Intent.Delete)

            Then("마지막 글자가 지워져 '12'가 되어야 한다") {
                viewModel.container.stateFlow.value.expression shouldBe "12"
            }
        }
    }

    Given("'10+20'이 입력된 상태에서") {
        every { calculatorEngine.calculate(any()) } returns "0"
        every { calculatorEngine.calculate("10+20") } returns "30"

        When("=을 누르면") {
            "10+20".forEach { ch ->
                val token = when {
                    ch.isDigit() -> CalculatorToken.Number(ch.toString())
                    else -> CalculatorToken.Operator(ch.toString())
                }
                viewModel.processIntent(CalculatorContract.Intent.Input(token))
            }
            viewModel.processIntent(CalculatorContract.Intent.Calculate)

            Then("결과 '30'이 expression에 표시되어야 한다") {
                viewModel.container.stateFlow.value.expression shouldBe "30"
            }
        }
    }

    Given("계산 결과가 에러일 때") {
        every { calculatorEngine.calculate(any()) } returns "Error"

        When("=을 누르면") {
            Then("에러 상태가 되고 스낵바 SideEffect가 발생해야 한다") {
                runTest {
                    val viewModel = CalculatorViewModel(calculatorEngine)

                    viewModel.test(this) {
                        expectInitialState()

                        // 1. 수식 입력
                        containerHost.processIntent(CalculatorContract.Intent.Input(CalculatorToken.Number("1")))
                        expectState { copy(expression = "1", cursorPosition = 1) }

                        // 2. 계산 액션 발생
                        containerHost.processIntent(CalculatorContract.Intent.Calculate)

                        // 3. 상태 검증 (에러 상태로 변경됨)
                        expectState {
                            copy(isError = true, errorMessage = "계산 오류")
                        }

                        // 4. Turbine 없이 Orbit 내장 기능으로 SideEffect 검증
                        expectSideEffect(
                            CalculatorContract.SideEffect.ShowSnackBar("계산할 수 없는 수식입니다.")
                        )
                    }
                }
            }
        }
    }
})