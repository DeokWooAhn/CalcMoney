package com.ahn.presentation.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.util.Locale

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

    given("환율 화면 받을 금액·즐겨찾기 카드 금액처럼 소수 둘째 자리 금액을 표시할 때") {
        `when`("천 단위 쉼표를 넣으면") {
            then("정수부에만 세 자리마다 쉼표를 넣고 소수부는 그대로 둔다") {
                formatNumberWithCommas("1343.87") shouldBe "1,343.87"
                formatNumberWithCommas("1000000.00") shouldBe "1,000,000.00"
            }

            then("기기 로케일이 독일어여도 정수부 구분 기호는 쉼표다") {
                val original = Locale.getDefault()
                try {
                    Locale.setDefault(Locale.GERMANY)

                    formatNumberWithCommas("1500.00") shouldBe "1,500.00"
                    formatNumberWithCommas("12000") shouldBe "12,000"
                } finally {
                    Locale.setDefault(original)
                }
            }

            then("천 미만 금액과 빈 금액 대신 쓰는 0은 그대로 둔다") {
                formatNumberWithCommas("999.99") shouldBe "999.99"
                formatNumberWithCommas("0") shouldBe "0"
                formatNumberWithCommas("") shouldBe ""
            }
        }
    }
})
