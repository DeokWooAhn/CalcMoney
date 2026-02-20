package com.ahn.presentation.ui.screen.exchange

import app.cash.turbine.test
import com.ahn.domain.usecase.GetExchangeRateUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModelTest : BehaviorSpec({

    val testDispatcher = StandardTestDispatcher()

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Dispatchers.resetMain() }

    Given("ViewModel이 처음 생성되었을 때") {
        Then("기본 통화는 USD -> KRW이어야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)

                advanceUntilIdle()

                viewModel.state.value.fromCurrency shouldBe Currency.USD
                viewModel.state.value.toCurrency shouldBe Currency.KRW
            }
        }

        Then("기본 금액은 '1'이어야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)

                advanceUntilIdle()

                viewModel.state.value.fromAmount shouldBe "1"
            }
        }
    }

    Given("금액을 입력할 때") {
        Then("유효한 금액을 입력하면 입력값과 계산 결과가 반영되어야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)
                advanceUntilIdle()

                viewModel.processIntent(ExchangeContract.Intent.UpdateFromAmount("1000"))
                advanceUntilIdle()

                viewModel.state.value.fromAmount shouldBe "1000"
                viewModel.state.value.toAmount shouldBe "1333330.00"
            }
        }

        Then("잘못된 형식의 금액을 입력하면 값이 변경되지 않아야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)
                advanceUntilIdle()

                viewModel.processIntent(ExchangeContract.Intent.UpdateFromAmount("abc"))
                advanceUntilIdle()

                viewModel.state.value.fromAmount shouldBe "1"
            }
        }
    }

    Given("통화를 선택할 때") {
        Then("출발 통화를 변경하면 상태가 반영되어야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)
                advanceUntilIdle()

                viewModel.processIntent(ExchangeContract.Intent.SelectFromCurrency(Currency.USD))
                advanceUntilIdle()

                viewModel.state.value.fromCurrency shouldBe Currency.USD
            }
        }

        Then("도착 통화를 변경하면 상태가 반영되어야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)
                advanceUntilIdle()

                viewModel.processIntent(ExchangeContract.Intent.SelectToCurrency(Currency.JPY))
                advanceUntilIdle()

                viewModel.state.value.toCurrency shouldBe Currency.JPY
            }
        }
    }

    Given("통화를 스왑할 때") {
        Then("스왑 후 통화가 바뀌고 fromAmount는 이전 toAmount여야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } returns 1333.33
                val viewModel = ExchangeViewModel(useCase)
                advanceUntilIdle()

                viewModel.processIntent(ExchangeContract.Intent.UpdateFromAmount("100"))
                advanceUntilIdle()
                val toAmountBeforeSwap = viewModel.state.value.toAmount

                viewModel.processIntent(ExchangeContract.Intent.SwapCurrencies)
                advanceUntilIdle()

                viewModel.state.value.fromCurrency shouldBe Currency.KRW
                viewModel.state.value.toCurrency shouldBe Currency.USD
                viewModel.state.value.fromAmount shouldBe toAmountBeforeSwap
            }
        }
    }

    Given("API 호출이 실패할 때") {
        Then("에러 Snackbar SideEffect가 표시되어야 한다") {
            runTest(testDispatcher) {
                val useCase = mockk<GetExchangeRateUseCase>()
                coEvery { useCase(any(), any()) } throws Exception("Network Error")
                val viewModel = ExchangeViewModel(useCase)

                viewModel.sideEffect.test {
                    viewModel.processIntent(ExchangeContract.Intent.SelectFromCurrency(Currency.EUR))
                    advanceUntilIdle()

                    awaitItem() shouldBe ExchangeContract.SideEffect.ShowSnackBar(
                        "환율 정보를 가져올 수 없습니다: Network Error",
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }
})
