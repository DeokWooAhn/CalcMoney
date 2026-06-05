package com.ahn.presentation.ui.screen.favorite

import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.usecase.CalculateExchangeAmountUseCase
import com.ahn.domain.exchange.usecase.ConvertExchangeAmountUseCase
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.exchange.usecase.GetExchangeRateUseCase
import com.ahn.domain.exchange.usecase.GetLatestExchangeRateDateUseCase
import com.ahn.domain.exchange.usecase.GetLatestExchangeRateFetchedAtUseCase
import com.ahn.domain.exchange.usecase.GetSupportedCurrenciesUseCase
import com.ahn.domain.exchange.usecase.RefreshExchangeRatesUseCase
import com.ahn.domain.favorite.usecase.BuildFavoriteRatesUseCase
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.domain.favorite.usecase.GetFavoriteCurrenciesUseCase
import com.ahn.domain.favorite.usecase.ToggleFavoriteCurrencyUseCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    lateinit var testDispatcher: TestDispatcher
    val usd = CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸")
    val krw = CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷")
    val jpy = CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵")
    val currencies = listOf(usd, krw, jpy)

    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()

    fun createViewModel() = FavoriteViewModel(
        exchangeUseCases = ExchangeUseCases(
            exchangeAmount = mockk<CalculateExchangeAmountUseCase>(),
            convertExchangeAmount = mockk<ConvertExchangeAmountUseCase>(),
            getExchangeRate = getExchangeRateUseCase,
            getLatestRateDate = mockk<GetLatestExchangeRateDateUseCase>(),
            getLatestFetchedAt = mockk<GetLatestExchangeRateFetchedAtUseCase>(),
            refreshExchangeRates = mockk<RefreshExchangeRatesUseCase>(),
            getSupportedCurrencies = mockk<GetSupportedCurrenciesUseCase>(),
        ),
        favoriteUseCases = FavoriteUseCases(
            buildFavoriteRates = BuildFavoriteRatesUseCase(),
            getFavoriteCurrencies = mockk<GetFavoriteCurrenciesUseCase>(),
            toggleFavoriteCurrency = mockk<ToggleFavoriteCurrencyUseCase>(),
        ),
    )

    beforeEach {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
    }
    afterEach { Dispatchers.resetMain() }

    Given("즐겨찾기 환율 ViewModel에서") {
        When("기준 통화가 없으면") {
            Then("즐겨찾기 목록을 비우고 환율을 조회하지 않아야 한다") {
                runTest(testDispatcher) {
                    val viewModel = createViewModel()

                    viewModel.onExchangeStateChanged(
                        fromCurrency = null,
                        favoriteCurrencyCodes = listOf("KRW"),
                        availableCurrencies = currencies,
                    )
                    advanceUntilIdle()

                    viewModel.state.value shouldBe FavoriteContract.State(
                        isLoading = false,
                        items = emptyList(),
                    )
                    coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
                }
            }
        }

        When("즐겨찾기 통화의 환율을 가져오면") {
            Then("환산 금액과 환율 라벨을 목록 상태에 반영해야 한다") {
                runTest(testDispatcher) {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1500.0
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5

                    val viewModel = createViewModel()

                    viewModel.onExchangeStateChanged(
                        fromCurrency = usd,
                        favoriteCurrencyCodes = listOf("KRW", "JPY"),
                        availableCurrencies = currencies,
                    )
                    advanceUntilIdle()

                    val state = viewModel.state.value
                    state.isLoading shouldBe false
                    state.items.map { it.currency.code } shouldContainExactly listOf("KRW", "JPY")
                    state.items[0].convertedAmount shouldBe "1500.00"
                    state.items[0].rateLabel shouldBe "1 USD = 1500.0000 KRW"
                    state.items[1].convertedAmount shouldBe "9.50"
                    state.items[1].rateLabel shouldBe "1 USD = 9.5000 JPY"
                }
            }
        }

        When("기준 금액이 변경되면") {
            Then("캐시된 환율을 이용해 즐겨찾기 금액만 다시 계산해야 한다") {
                runTest(testDispatcher) {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1500.0

                    val viewModel = createViewModel()

                    viewModel.onExchangeStateChanged(
                        fromCurrency = usd,
                        favoriteCurrencyCodes = listOf("KRW"),
                        availableCurrencies = currencies,
                    )
                    advanceUntilIdle()
                    viewModel.onBaseAmountChanged("2.5")

                    viewModel.state.value.items.single().convertedAmount shouldBe "3750.00"
                    coVerify(exactly = 1) { getExchangeRateUseCase("USD", "KRW") }
                }
            }
        }

        When("즐겨찾기에 기준 통화, 없는 통화, 실패한 통화가 섞여 있으면") {
            Then("성공적으로 조회된 통화만 목록에 보여야 한다") {
                runTest(testDispatcher) {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1500.0
                    coEvery { getExchangeRateUseCase("USD", "JPY") } throws IllegalStateException("rate not found")

                    val viewModel = createViewModel()

                    viewModel.onExchangeStateChanged(
                        fromCurrency = usd,
                        favoriteCurrencyCodes = listOf("USD", "KRW", "JPY", "EUR"),
                        availableCurrencies = currencies,
                    )
                    advanceUntilIdle()

                    val items = viewModel.state.value.items
                    items.map { it.currency.code } shouldContainExactly listOf("KRW")
                    items.single().convertedAmount shouldBe "1500.00"
                    coVerify(exactly = 0) { getExchangeRateUseCase("USD", "USD") }
                    coVerify(exactly = 1) { getExchangeRateUseCase("USD", "KRW") }
                    coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }
                    coVerify(exactly = 0) { getExchangeRateUseCase("USD", "EUR") }
                }
            }
        }

        When("즐겨찾기 조건이 사라지면") {
            Then("이전 목록과 캐시를 비워야 한다") {
                runTest(testDispatcher) {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1500.0

                    val viewModel = createViewModel()

                    viewModel.onExchangeStateChanged(
                        fromCurrency = usd,
                        favoriteCurrencyCodes = listOf("KRW"),
                        availableCurrencies = currencies,
                    )
                    advanceUntilIdle()
                    viewModel.onExchangeStateChanged(
                        fromCurrency = usd,
                        favoriteCurrencyCodes = emptyList(),
                        availableCurrencies = currencies,
                    )
                    advanceUntilIdle()
                    viewModel.onBaseAmountChanged("10")

                    viewModel.state.value shouldBe FavoriteContract.State(
                        isLoading = false,
                        items = emptyList(),
                    )
                }
            }
        }
    }
})
