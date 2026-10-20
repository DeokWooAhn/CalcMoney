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

        Given("통화를 검색할 때") {
            // 화면 언어 이름과 영어 이름을 함께 검색한다.
            val names = mapOf(
                "USD" to listOf("미국 달러", "US Dollar"),
                "AUD" to listOf("호주 달러", "Australian Dollar"),
                "CAD" to listOf("캐나다 달러", "Canadian Dollar"),
                "JPY" to listOf("일본 엔", "Japanese Yen"),
                "SAR" to listOf("사우디아라비아 리얄", "Saudi Riyal"),
                "VND" to listOf("베트남 동", "Vietnamese Dong"),
            )
            val searchable = names.keys.map(::currency)

            fun search(query: String, favorites: List<String> = emptyList()) =
                searchCurrencies(searchable, query, favorites) { names.getValue(it.code) }.map { it.code }

            When("나라 이름으로 찾으면") {
                Then("이름에 그 나라가 들어간 통화를 찾아야 한다") {
                    search("베트남") shouldBe listOf("VND")
                }
            }

            When("통화 코드를 소문자로 찾으면") {
                Then("대소문자와 관계없이 코드가 같은 통화를 찾아야 한다") {
                    search("jpy") shouldBe listOf("JPY")
                }
            }

            When("코드와 이름에 모두 걸리는 검색어로 찾으면") {
                // 알파벳순(ABC, ABCD, QQQ, XYZ)과 다르게 나와야 순위대로 정렬한 것이다.
                val ranked = listOf("QQQ", "XYZ", "ABCD", "ABC").map(::currency)
                val rankedNames = mapOf(
                    "ABC" to listOf("Alpha"),
                    "ABCD" to listOf("Beta"),
                    "XYZ" to listOf("Abc Dollar"),
                    "QQQ" to listOf("My Abc"),
                )
                val result = searchCurrencies(ranked, "abc", emptyList()) { rankedNames.getValue(it.code) }

                Then("코드가 같은 것, 코드로 시작하는 것, 이름으로 시작하는 것, 이름에 들어간 것 순으로 둬야 한다") {
                    result.map { it.code } shouldBe listOf("ABC", "ABCD", "XYZ", "QQQ")
                }
            }

            When("이름에 같은 낱말이 들어간 통화가 여럿이면") {
                Then("즐겨찾기를 먼저 두고 나머지는 통화 코드 알파벳순으로 둬야 한다") {
                    search("달러") shouldBe listOf("AUD", "CAD", "USD")
                    search("달러", favorites = listOf("USD")) shouldBe listOf("USD", "AUD", "CAD")
                }
            }

            When("공백을 빼고 찾으면") {
                Then("공백을 무시하고 이름을 찾아야 한다") {
                    search("미국달러") shouldBe listOf("USD")
                }
            }

            When("검색어가 비어 있거나 맞는 통화가 없으면") {
                Then("빈 목록을 돌려줘야 한다") {
                    search("  ") shouldBe emptyList()
                    search("없는통화") shouldBe emptyList()
                }
            }
        }
    })

private fun currency(code: String): CurrencyInfo {
    return CurrencyInfo(code = code, displayCode = code, name = "$code 통화", flagEmoji = "🌐")
}
