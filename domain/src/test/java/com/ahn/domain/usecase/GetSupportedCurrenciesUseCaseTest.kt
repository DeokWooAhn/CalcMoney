package com.ahn.domain.usecase

import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.repository.ExchangeRateRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class GetSupportedCurrenciesUseCaseTest : DescribeSpec({

    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<ExchangeRateRepository>()
    val useCase = GetSupportedCurrenciesUseCase(repository)

    beforeEach {
        clearAllMocks()
    }

    describe("지원 통화 목록 조회") {
        it("repository에서 조회한 지원 통화 목록을 그대로 반환한다") {
            val supportedCurrencies = listOf(
                CurrencyInfo(
                    code = "KRW",
                    displayCode = "KRW",
                    name = "대한민국 원",
                    flagEmoji = "🇰🇷",
                ),
                CurrencyInfo(
                    code = "USD",
                    displayCode = "USD",
                    name = "미국 달러",
                    flagEmoji = "🇺🇸",
                ),
            )

            coEvery {
                repository.getSupportedCurrencies()
            } returns supportedCurrencies

            val result = useCase()

            result shouldBe supportedCurrencies
            coVerify(exactly = 1) {
                repository.getSupportedCurrencies()
            }
        }
    }
})
