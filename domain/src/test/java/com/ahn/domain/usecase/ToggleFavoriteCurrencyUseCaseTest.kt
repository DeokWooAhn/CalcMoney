package com.ahn.domain.usecase

import com.ahn.domain.repository.FavoriteCurrencyRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class ToggleFavoriteCurrencyUseCaseTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<FavoriteCurrencyRepository>()
    val useCase = ToggleFavoriteCurrencyUseCase(repository)

    beforeEach {
        clearAllMocks()
    }

    Given("선택한 통화가 즐겨찾기에 없는 상태에서") {
        When("즐겨찾기를 토글하면") {
            Then("해당 통화를 즐겨찾기에 추가해야 한다") {
                val currencyCode = "USD"

                coEvery { repository.isFavorite(currencyCode) } returns false
                coEvery { repository.addFavorite(currencyCode) } returns Unit

                useCase(currencyCode)

                coVerify(exactly = 1) { repository.isFavorite(currencyCode) }
                coVerify(exactly = 1) { repository.addFavorite(currencyCode) }
                coVerify(exactly = 0) { repository.removeFavorite(currencyCode) }
            }
        }
    }

    Given("선택한 통화가 이미 즐겨찾기에 있는 상태에서") {
        When("즐겨찾기를 토글하면") {
            Then("해당 통화를 즐겨찾기에서 제거해야 한다") {
                val currencyCode = "USD"

                coEvery { repository.isFavorite(currencyCode) } returns true
                coEvery { repository.removeFavorite(currencyCode) } returns Unit

                useCase(currencyCode)

                coVerify(exactly = 1) { repository.isFavorite(currencyCode) }
                coVerify(exactly = 1) { repository.removeFavorite(currencyCode) }
                coVerify(exactly = 0) { repository.addFavorite(currencyCode) }
            }
        }
    }
})
