package com.ahn.data.exchange.mapper

import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.domain.currency.model.CurrencyInfo
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExchangeRateMapperTest :
    DescribeSpec({
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

            it("국가를 특정할 수 없는 통화 코드는 중립 아이콘을 사용한다") {
                exchangeRateEntity(
                    code = "XXX",
                    currencyName = "Unknown Currency",
                ).toCurrencyInfo() shouldBe CurrencyInfo(
                    code = "XXX",
                    displayCode = "XXX",
                    name = "Unknown Currency",
                    flagEmoji = "🌐",
                )
            }
        }

        describe("통화 코드의 국기 이모지") {
            it("통화 코드 앞 두 글자의 국가 국기를 사용한다") {
                getFlagEmoji("TWD") shouldBe "🇹🇼"
                getFlagEmoji("VND") shouldBe "🇻🇳"
                getFlagEmoji("CNY") shouldBe "🇨🇳"
                getFlagEmoji("CNH") shouldBe "🇨🇳"
            }

            it("대소문자를 구분하지 않는다") {
                getFlagEmoji("php") shouldBe "🇵🇭"
            }

            it("앞 두 글자가 국가 코드가 아닌 통화는 예외 매핑을 사용한다") {
                getFlagEmoji("EUR") shouldBe "🇪🇺"
                getFlagEmoji("ANG") shouldBe "🇨🇼"
            }

            it("ANG의 후속 통화인 카리브 길더는 퀴라소 국기를 사용한다") {
                getFlagEmoji("XCG") shouldBe "🇨🇼"
            }

            it("여러 나라가 함께 쓰는 X로 시작하는 통화는 중립 아이콘을 사용한다") {
                listOf("XAF", "XOF", "XCD", "XPF", "XDR").forEach { code ->
                    getFlagEmoji(code) shouldBe NO_COUNTRY_FLAG
                }
            }

            it("앞 두 글자가 국가 코드가 아니면 중립 아이콘을 사용한다") {
                getFlagEmoji("QQQ") shouldBe NO_COUNTRY_FLAG
                getFlagEmoji("US") shouldBe NO_COUNTRY_FLAG
            }
        }
    })

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
