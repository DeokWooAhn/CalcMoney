package com.ahn.presentation.ui.component

import com.ahn.domain.currency.model.CurrencyInfo
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CurrencyPickerListTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        // 서버가 알파벳순이 아닌 순서로 줘도 화면에서는 정렬돼야 한다.
        val currencies = listOf("USD", "KRW", "AED", "JPY", "EUR", "AUD", "JEP").map(::currency)

        Given("즐겨찾기가 없을 때") {
            When("목록을 만들면") {
                val pickerList = buildCurrencyPickerList(currencies, favoriteCodesForSort = emptyList())

                Then("통화 코드 알파벳순으로 정렬해야 한다") {
                    pickerList.currencies.map { it.code } shouldBe
                        listOf("AED", "AUD", "EUR", "JEP", "JPY", "KRW", "USD")
                }

                Then("각 글자 구간이 시작하는 위치를 인덱스로 가져야 한다") {
                    pickerList.indexPositions.toList() shouldBe listOf(
                        "A" to 0,
                        "E" to 2,
                        "J" to 3,
                        "K" to 5,
                        "U" to 6,
                    )
                }

                Then("즐겨찾기 인덱스는 없어야 한다") {
                    pickerList.indexPositions.containsKey(FAVORITE_INDEX_LABEL) shouldBe false
                }
            }
        }

        Given("즐겨찾기가 있을 때") {
            When("목록을 만들면") {
                val pickerList = buildCurrencyPickerList(currencies, favoriteCodesForSort = listOf("USD", "JPY"))

                Then("즐겨찾기를 등록 순서대로 맨 위에 두고 나머지는 알파벳순으로 이어야 한다") {
                    pickerList.currencies.map { it.code } shouldBe
                        listOf("USD", "JPY", "AED", "AUD", "EUR", "JEP", "KRW")
                }

                Then("즐겨찾기 인덱스가 맨 앞에서 시작하고 글자 인덱스는 즐겨찾기 뒤부터 세야 한다") {
                    pickerList.indexPositions.toList() shouldBe listOf(
                        FAVORITE_INDEX_LABEL to 0,
                        "A" to 2,
                        "E" to 4,
                        "J" to 5,
                        "K" to 6,
                    )
                }
            }

            When("즐겨찾기 코드가 중복되거나 목록에 없는 코드가 섞여 있으면") {
                val pickerList = buildCurrencyPickerList(
                    currencies,
                    favoriteCodesForSort = listOf("EUR", "ZZZ", "EUR", "AED"),
                )

                Then("처음 등록한 순서를 따르고 없는 코드는 무시해야 한다") {
                    pickerList.currencies.take(2).map { it.code } shouldBe listOf("EUR", "AED")
                    pickerList.currencies.size shouldBe currencies.size
                }
            }
        }

        Given("알파벳으로 시작하지 않는 코드가 있을 때") {
            When("목록을 만들면") {
                val pickerList = buildCurrencyPickerList(
                    listOf("USD", "1AB", "aud").map(::currency),
                    favoriteCodesForSort = emptyList(),
                )

                Then("대소문자와 관계없이 정렬하고 알파벳이 아닌 코드는 # 구간으로 맨 뒤에 둬야 한다") {
                    pickerList.currencies.map { it.code } shouldBe listOf("aud", "USD", "1AB")
                    pickerList.indexPositions.toList() shouldBe listOf(
                        "A" to 0,
                        "U" to 1,
                        OTHER_INDEX_LABEL to 2,
                    )
                }
            }
        }
    })

private fun currency(code: String): CurrencyInfo {
    return CurrencyInfo(code = code, displayCode = code, name = "$code 통화", flagEmoji = "🌐")
}
