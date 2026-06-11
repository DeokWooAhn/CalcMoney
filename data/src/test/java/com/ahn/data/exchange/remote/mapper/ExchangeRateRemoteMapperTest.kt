package com.ahn.data.exchange.remote.mapper

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe

class ExchangeRateRemoteMapperTest :
    DescribeSpec({
        describe("Firestore 환율 문서 변환") {
            it("Number 타입 baseRate를 환율 Entity로 변환한다") {
                val entities = listOf(
                    mapOf(
                        "code" to "USD",
                        "currencyUnit" to "USD",
                        "currencyName" to "US Dollar",
                        "baseRate" to 1400.25,
                    ),
                ).toExchangeRateEntities(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entities shouldHaveSize 1
                entities.first().code shouldBe "USD"
                entities.first().currencyUnit shouldBe "USD"
                entities.first().currencyName shouldBe "US Dollar"
                entities.first().baseRate shouldBeExactly 1400.25
                entities.first().fetchedAt shouldBe 1000L
                entities.first().rateDate shouldBe "20260601"
            }

            it("String 타입 baseRate의 쉼표를 제거하고 변환한다") {
                val entities = listOf(
                    mapOf(
                        "code" to "JPY",
                        "baseRate" to "9.5012",
                    ),
                    mapOf(
                        "code" to "USD",
                        "baseRate" to "1,390.80",
                    ),
                ).toExchangeRateEntities(
                    fetchedAt = 2000L,
                    rateDate = "20260602",
                )

                entities shouldHaveSize 2
                entities[0].baseRate shouldBeExactly 9.5012
                entities[1].baseRate shouldBeExactly 1390.80
            }

            it("code나 baseRate가 없으면 해당 항목을 제외한다") {
                val entities = listOf(
                    mapOf("code" to "USD"),
                    mapOf("baseRate" to 1400.0),
                    mapOf("code" to "EUR", "baseRate" to "invalid"),
                    mapOf("code" to "JPY", "baseRate" to 9.5),
                ).toExchangeRateEntities(
                    fetchedAt = 3000L,
                    rateDate = "20260603",
                )

                entities shouldHaveSize 1
                entities.first().code shouldBe "JPY"
            }

            it("rates 필드가 비어 있거나 목록이 아니면 빈 목록을 반환한다") {
                emptyList<Any>().toExchangeRateEntities(
                    fetchedAt = 4000L,
                    rateDate = "20260604",
                ).shouldBeEmpty()

                mapOf("code" to "USD").toExchangeRateEntities(
                    fetchedAt = 4000L,
                    rateDate = "20260604",
                ).shouldBeEmpty()
            }
        }
    })
