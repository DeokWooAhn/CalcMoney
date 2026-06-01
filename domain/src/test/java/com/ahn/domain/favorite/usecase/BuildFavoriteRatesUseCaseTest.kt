package com.ahn.domain.favorite.usecase

import com.ahn.domain.currency.model.CurrencyInfo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe

class BuildFavoriteRatesUseCaseTest :
    DescribeSpec({

        val useCase = BuildFavoriteRatesUseCase()

        val krw = CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷")
        val usd = CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸")
        val jpy = CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵")

        describe("즐겨찾기 환율 목록 생성") {
            it("즐겨찾기 통화와 환율 정보를 이용해 변환 금액을 계산한다") {
                val result = useCase(
                    baseCurrency = krw,
                    baseAmount = "1000",
                    favoriteCurrencyCodes = listOf("USD", "JPY"),
                    availableCurrencies = listOf(krw, usd, jpy),
                    ratesByCode = mapOf(
                        "USD" to 0.00072,
                        "JPY" to 0.11,
                    ),
                )

                result.map { it.currency.code } shouldContainExactly listOf("USD", "JPY")
                result[0].baseCurrencyCode shouldBe "KRW"
                result[0].rate shouldBe (0.00072 plusOrMinus 0.000001)
                result[0].convertedAmount shouldBe (0.72 plusOrMinus 0.000001)
                result[1].convertedAmount shouldBe (110.0 plusOrMinus 0.000001)
            }

            it("기준 통화와 같은 즐겨찾기는 목록에서 제외한다") {
                val result = useCase(
                    baseCurrency = krw,
                    baseAmount = "1000",
                    favoriteCurrencyCodes = listOf("KRW", "USD"),
                    availableCurrencies = listOf(krw, usd),
                    ratesByCode = mapOf("USD" to 0.00072),
                )

                result.map { it.currency.code } shouldContainExactly listOf("USD")
            }

            it("통화 정보가 없거나 환율이 없는 즐겨찾기는 제외한다") {
                val result = useCase(
                    baseCurrency = krw,
                    baseAmount = "1000",
                    favoriteCurrencyCodes = listOf("USD", "JPY", "VND"),
                    availableCurrencies = listOf(krw, usd, jpy),
                    ratesByCode = mapOf("USD" to 0.00072),
                )

                result.map { it.currency.code } shouldContainExactly listOf("USD")
            }

            it("기준 금액이 숫자가 아니면 0으로 계산한다") {
                val result = useCase(
                    baseCurrency = krw,
                    baseAmount = "",
                    favoriteCurrencyCodes = listOf("USD"),
                    availableCurrencies = listOf(krw, usd),
                    ratesByCode = mapOf("USD" to 0.00072),
                )

                result.first().convertedAmount shouldBe 0.0
            }

            it("기준 통화가 없으면 빈 목록을 반환한다") {
                val result = useCase(
                    baseCurrency = null,
                    baseAmount = "1000",
                    favoriteCurrencyCodes = listOf("USD"),
                    availableCurrencies = listOf(usd),
                    ratesByCode = mapOf("USD" to 0.00072),
                )

                result shouldBe emptyList()
            }
        }
    })
