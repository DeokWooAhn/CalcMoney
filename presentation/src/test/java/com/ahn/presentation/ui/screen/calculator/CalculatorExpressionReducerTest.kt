package com.ahn.presentation.ui.screen.calculator

import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import com.ahn.domain.exchange.usecase.ConvertExchangeAmountUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk

class CalculatorExpressionReducerTest : BehaviorSpec({

    val calculateExpressionUseCase = mockk<CalculateExpressionUseCase>()
    val convertExchangeAmountUseCase = mockk<ConvertExchangeAmountUseCase>()

    fun createReducer() = CalculatorExpressionReducer(
        calculateExpression = calculateExpressionUseCase,
        convertExchangeAmount = convertExchangeAmountUseCase,
    )

    fun updatedState(result: CalculatorExpressionResult): CalculatorContract.State {
        return (result as CalculatorExpressionResult.Updated).state
    }

    beforeEach {
        clearAllMocks()
        every { calculateExpressionUseCase.calculate(any()) } returns "0"
        every { convertExchangeAmountUseCase.convertExpression(any(), any(), any()) } returns ""
        every { convertExchangeAmountUseCase.convertSingleAmount(any(), any(), any()) } returns ""
    }

    given("계산식이 비어 있을 때") {
        `when`("마이너스 연산자를 입력하면") {
            then("음수 입력을 시작해야 한다") {
                val reducer = createReducer()

                val minusState = checkNotNull(
                    reducer.inputOperator(
                        currentState = CalculatorContract.State(),
                        operator = "-",
                    ),
                )

                minusState.expression shouldBe "-"
                minusState.cursorPosition shouldBe 1

                val numberState = updatedState(reducer.inputNumber(minusState, "3"))

                numberState.expression shouldBe "-3"
                numberState.cursorPosition shouldBe 2
            }
        }
    }

    given("곱하기 연산자 뒤에서") {
        `when`("마이너스 연산자를 입력하면") {
            then("이전 연산자를 교체하지 않고 음수 연산자로 추가해야 한다") {
                every { calculateExpressionUseCase.calculate("2×-3") } returns "-6"

                val reducer = createReducer()
                val multipliedState = CalculatorContract.State(
                    expression = "2×",
                    cursorPosition = 2,
                )

                val minusState = checkNotNull(
                    reducer.inputOperator(
                        currentState = multipliedState,
                        operator = "-",
                    ),
                )

                minusState.expression shouldBe "2×-"
                minusState.cursorPosition shouldBe 3

                val numberState = updatedState(reducer.inputNumber(minusState, "3"))

                numberState.expression shouldBe "2×-3"
                numberState.cursorPosition shouldBe 4
                numberState.previewResult shouldBe "-6"
            }
        }
    }

    given("여는 괄호 뒤에서") {
        `when`("마이너스 연산자를 입력하면") {
            then("괄호 안 음수 입력을 허용해야 한다") {
                val reducer = createReducer()
                val openedParenthesisState = CalculatorContract.State(
                    expression = "(",
                    cursorPosition = 1,
                )

                val minusState = checkNotNull(
                    reducer.inputOperator(
                        currentState = openedParenthesisState,
                        operator = "-",
                    ),
                )

                minusState.expression shouldBe "(-"
                minusState.cursorPosition shouldBe 2

                val numberState = updatedState(reducer.inputNumber(minusState, "4"))

                numberState.expression shouldBe "(-4"
                numberState.cursorPosition shouldBe 3

                val closedParenthesisState = reducer.inputParenthesis(numberState)

                closedParenthesisState.expression shouldBe "(-4)"
                closedParenthesisState.cursorPosition shouldBe 4
            }
        }
    }
})
