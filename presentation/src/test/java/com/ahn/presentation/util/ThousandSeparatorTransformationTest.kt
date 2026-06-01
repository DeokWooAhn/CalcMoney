package com.ahn.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ThousandSeparatorTransformationTest : BehaviorSpec({

    val operatorColor = Color(0xFFFF9800)
    val transformation = ThousandSeparatorTransformation(operatorColor)

    fun transformedText(input: String): AnnotatedString {
        return transformation.filter(AnnotatedString(input)).text
    }

    given("계산식 맨 앞에 마이너스가 있을 때") {
        `when`("텍스트를 변환하면") {
            then("맨 앞 마이너스에는 연산자 색상을 적용하지 않아야 한다") {
                val text = transformedText("-5+6")

                text.text shouldBe "\u200B-5\u200B+6"
                text.spanStyles.map { it.start to it.end } shouldBe listOf(4 to 5)
            }
        }
    }

    given("다른 연산자 뒤에 마이너스가 있을 때") {
        `when`("텍스트를 변환하면") {
            then("음수 마이너스에는 연산자 색상을 적용하지 않아야 한다") {
                val text = transformedText("2×-3")

                text.text shouldBe "2\u200B×\u200B-3"
                text.spanStyles.map { it.start to it.end } shouldBe listOf(2 to 3)
            }
        }
    }

    given("숫자 뒤에 마이너스가 있을 때") {
        `when`("텍스트를 변환하면") {
            then("빼기 연산자에는 연산자 색상을 적용해야 한다") {
                val text = transformedText("5-3")

                text.text shouldBe "5\u200B-3"
                text.spanStyles.map { it.start to it.end } shouldBe listOf(2 to 3)
            }
        }
    }
})
