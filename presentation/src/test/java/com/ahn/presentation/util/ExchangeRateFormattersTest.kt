package com.ahn.presentation.util

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExchangeRateFormattersTest :
    DescribeSpec({
        describe("환율 기준 날짜 포맷") {
            it("yyyyMMdd 형식이면 점으로 구분된 날짜로 변환한다") {
                formatExchangeRateDate("20260529") shouldBe "2026.05.29"
            }

            it("yyyyMMdd 형식이 아니면 원본을 반환한다") {
                formatExchangeRateDate("2026.05.29") shouldBe "2026.05.29"
            }
        }

        describe("환율 갱신 시간 포맷") {
            it("저장된 시간이 없으면 null을 반환한다") {
                formatExchangeRateFetchedAt(0L) shouldBe null
                formatExchangeRateFetchedAt(-1L) shouldBe null
            }

            it("저장된 시간이 있으면 날짜와 시간 형식으로 변환한다") {
                val formatted = formatExchangeRateFetchedAt(1_779_999_120_000L)

                Regex("""\d{4}\.\d{2}\.\d{2} \d{2}:\d{2}""").matches(formatted.orEmpty()) shouldBe true
            }
        }
    })
