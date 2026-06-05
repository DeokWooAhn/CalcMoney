package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.data.exchange.remote.dto.ExchangeRateResponse
import com.ahn.domain.currency.model.CurrencyInfo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ExchangeRateMapperTest :
    DescribeSpec({
        describe("환율 응답 Entity 변환") {
            it("유효한 응답을 Entity로 변환한다") {
                val entity = exchangeRateResponse(
                    currencyUnit = "USD",
                    currencyName = "US Dollar",
                    baseRate = "1,400.0",
                ).toEntity(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entity shouldBe ExchangeRateEntity(
                    code = "USD",
                    currencyUnit = "USD",
                    currencyName = "US Dollar",
                    baseRate = 1400.0,
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )
            }

            it("100단위 통화 응답은 환율을 100으로 나누고 통화 코드를 정규화한다") {
                val entity = exchangeRateResponse(
                    currencyUnit = "JPY(100)",
                    currencyName = "Japanese Yen",
                    baseRate = "950.12",
                ).toEntity(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entity?.code shouldBe "JPY"
                entity?.currencyUnit shouldBe "JPY(100)"
                entity shouldNotBe null
                entity!!.baseRate shouldBeExactly 9.5012
            }

            it("통화명이 없으면 Unknown으로 변환한다") {
                val entity = exchangeRateResponse(
                    currencyUnit = "USD",
                    currencyName = null,
                    baseRate = "1,400.0",
                ).toEntity(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entity?.currencyName shouldBe "Unknown"
            }

            it("성공 결과가 아니면 null을 반환한다") {
                val entity = exchangeRateResponse(result = 3).toEntity(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entity shouldBe null
            }

            it("통화 단위가 비어 있으면 null을 반환한다") {
                val entity = exchangeRateResponse(currencyUnit = "").toEntity(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entity shouldBe null
            }

            it("환율 문자열을 숫자로 파싱할 수 없으면 null을 반환한다") {
                val entity = exchangeRateResponse(baseRate = "abc").toEntity(
                    fetchedAt = 1000L,
                    rateDate = "20260601",
                )

                entity shouldBe null
            }
        }

        describe("환율 Entity 목록") {
            it("KRW 환율은 항상 1.0으로 반환한다") {
                emptyList<ExchangeRateEntity>().rateOf("KRW") shouldBe 1.0
            }

            it("지정한 통화의 기준 환율을 반환한다") {
                val rates = listOf(
                    exchangeRateEntity(code = "USD", baseRate = 1400.0),
                    exchangeRateEntity(code = "JPY", baseRate = 9.5),
                )

                rates.rateOf("JPY") shouldBe 9.5
            }

            it("목록에 없는 통화는 null을 반환한다") {
                val rates = listOf(exchangeRateEntity(code = "USD", baseRate = 1400.0))

                rates.rateOf("EUR") shouldBe null
            }
        }

        describe("통화 정보 변환") {
            it("Entity를 CurrencyInfo로 변환한다") {
                exchangeRateEntity(
                    code = "USD",
                    currencyName = "US Dollar",
                ).toCurrencyInfo() shouldBe CurrencyInfo(
                    code = "USD",
                    displayCode = "USD",
                    name = "US Dollar",
                    flagEmoji = "🇺🇸",
                )
            }

            it("KRW 통화 정보를 반환한다") {
                krwCurrencyInfo() shouldBe CurrencyInfo(
                    code = "KRW",
                    displayCode = "KRW",
                    name = "한국 원",
                    flagEmoji = "🇰🇷",
                )
            }

            it("지원하지 않는 통화 코드는 빈 국기 이모지를 사용한다") {
                exchangeRateEntity(
                    code = "XXX",
                    currencyName = "Unknown Currency",
                ).toCurrencyInfo() shouldBe CurrencyInfo(
                    code = "XXX",
                    displayCode = "XXX",
                    name = "Unknown Currency",
                    flagEmoji = "",
                )
            }
        }
    })

private fun exchangeRateResponse(
    result: Int? = 1,
    currencyUnit: String? = "USD",
    currencyName: String? = "US Dollar",
    baseRate: String? = "1,400.0",
): ExchangeRateResponse {
    return ExchangeRateResponse(
        result = result,
        currencyUnit = currencyUnit,
        currencyName = currencyName,
        baseRate = baseRate,
    )
}

private fun exchangeRateEntity(
    code: String,
    currencyName: String = "$code Currency",
    baseRate: Double = 1000.0,
): ExchangeRateEntity {
    return ExchangeRateEntity(
        code = code,
        currencyUnit = code,
        currencyName = currencyName,
        baseRate = baseRate,
        fetchedAt = 1000L,
        rateDate = "20260601",
    )
}
