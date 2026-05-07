package com.ahn.domain.usecase

import com.ahn.domain.repository.ExchangeRateRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class GetExchangeRateUseCaseTest : DescribeSpec({

    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<ExchangeRateRepository>()
    val useCase = GetExchangeRateUseCase(repository)

    beforeEach {
        clearAllMocks()
    }

    describe("환율 조회") {
        it("repository에서 조회한 환율을 그대로 반환한다") {
            val fromCurrencyCode = "KRW"
            val toCurrencyCode = "USD"
            val exchangeRate = 0.00072

            coEvery {
                repository.getExchangeRate(fromCurrencyCode, toCurrencyCode)
            } returns exchangeRate

            val result = useCase(fromCurrencyCode, toCurrencyCode)

            result shouldBe exchangeRate
            coVerify(exactly = 1) {
                repository.getExchangeRate(fromCurrencyCode, toCurrencyCode)
            }
        }
    }
})
