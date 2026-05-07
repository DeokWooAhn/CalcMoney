package com.ahn.domain.usecase

import com.ahn.domain.repository.FavoriteCurrencyRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class GetFavoriteCurrenciesUseCaseTest : DescribeSpec({

    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<FavoriteCurrencyRepository>()
    val useCase = GetFavoriteCurrenciesUseCase(repository)

    beforeEach {
        clearAllMocks()
    }

    describe("즐겨찾기 통화 목록 조회") {
        it("repository에서 받은 즐겨찾기 통화 코드 Flow를 그대로 반환한다") {
            val favoriteCurrencyCodes = listOf("USD", "JPY", "VND")

            every {
                repository.getFavoriteCurrencyCodes()
            } returns flowOf(favoriteCurrencyCodes)

            val result = useCase().first()

            result shouldBe favoriteCurrencyCodes
            verify(exactly = 1) {
                repository.getFavoriteCurrencyCodes()
            }
        }
    }
})
