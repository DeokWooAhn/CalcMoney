package com.ahn.presentation.ui.screen.calculator

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch

/**
 * Maestro Flow가 이 id로 키를 찾고, iOS `CalculatorKey.testID`와 같은 값이어야 Flow 하나로
 * 두 플랫폼을 돌릴 수 있다. 값을 바꾸면 `.maestro` Flow와 iOS도 함께 바꿔야 한다.
 */
class CalculatorKeyTestTagTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        Given("계산기 키패드 배열에서") {
            val tags = calculatorKeyRows.flatten().map { it.testTag }

            When("각 키의 E2E 테스트 id를 읽으면") {
                Then("배열 순서대로 약속한 id를 가져야 한다") {
                    tags shouldBe listOf(
                        "keypad.history", "keypad.clear", "keypad.parenthesis", "keypad.divide",
                        "keypad.7", "keypad.8", "keypad.9", "keypad.multiply",
                        "keypad.4", "keypad.5", "keypad.6", "keypad.minus",
                        "keypad.1", "keypad.2", "keypad.3", "keypad.plus",
                        "keypad.dot", "keypad.0", "keypad.delete", "keypad.equals",
                    )
                }

                // Maestro는 id를 정규식으로 해석한다. 기호가 섞이면 다른 키와 매칭된다.
                Then("정규식 특수문자 없이 영문 소문자와 숫자로만 이뤄져야 한다") {
                    tags.forEach { tag -> tag shouldMatch Regex("""keypad\.[a-z0-9]+""") }
                }

                Then("서로 겹치지 않아야 한다") {
                    tags.toSet().size shouldBe tags.size
                }
            }
        }
    })
