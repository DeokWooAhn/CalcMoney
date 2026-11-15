package com.ahn.presentation.ui.screen.exchange

import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.currency.model.ExchangeCurrencySelection
import com.ahn.domain.currency.usecase.CurrencySelectionUseCases
import com.ahn.domain.currency.usecase.GetCalculatorSelectionUseCase
import com.ahn.domain.currency.usecase.GetExchangeSelectionUseCase
import com.ahn.domain.currency.usecase.SaveCalculatorMainCurrencyUseCase
import com.ahn.domain.currency.usecase.SaveCalculatorSelectionUseCase
import com.ahn.domain.currency.usecase.SaveCalculatorSubCurrencyUseCase
import com.ahn.domain.currency.usecase.SaveExchangeFromCurrencyUseCase
import com.ahn.domain.currency.usecase.SaveExchangeSelectionUseCase
import com.ahn.domain.currency.usecase.SaveExchangeToCurrencyUseCase
import com.ahn.domain.exchange.model.ExchangeRateException
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
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeStaleResponseTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    lateinit var testDispatcher: TestDispatcher

    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()
    val getLatestExchangeRateDateUseCase = mockk<GetLatestExchangeRateDateUseCase>()
    val getLatestExchangeRateFetchedAtUseCase = mockk<GetLatestExchangeRateFetchedAtUseCase>()
    val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
    val calculateExchangeAmountUseCase = mockk<CalculateExchangeAmountUseCase>()
    val getFavoriteCurrenciesUseCase = mockk<GetFavoriteCurrenciesUseCase>()
    val getExchangeSelectionUseCase = mockk<GetExchangeSelectionUseCase>()
    val saveExchangeToCurrencyUseCase = mockk<SaveExchangeToCurrencyUseCase>()

    val mockCurrencies = listOf(
        CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸"),
        CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷"),
        CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵"),
    )
    val jpy = mockCurrencies[2]

    val rateFailure = ExchangeContract.Failure(
        scope = ExchangeContract.Failure.Scope.RATE,
        message = UiText.StringResource(R.string.exchange_rate_temporarily_unavailable),
    )

    fun createViewModel() = ExchangeViewModel(
        exchangeUseCases = ExchangeUseCases(
            exchangeAmount = calculateExchangeAmountUseCase,
            convertExchangeAmount = mockk<ConvertExchangeAmountUseCase>(),
            getExchangeRate = getExchangeRateUseCase,
            getLatestRateDate = getLatestExchangeRateDateUseCase,
            getLatestFetchedAt = getLatestExchangeRateFetchedAtUseCase,
            refreshExchangeRates = mockk<RefreshExchangeRatesUseCase>(),
            getSupportedCurrencies = getSupportedCurrenciesUseCase,
        ),
        favoriteUseCases = FavoriteUseCases(
            buildFavoriteRates = mockk<BuildFavoriteRatesUseCase>(),
            getFavoriteCurrencies = getFavoriteCurrenciesUseCase,
            toggleFavoriteCurrency = mockk<ToggleFavoriteCurrencyUseCase>(),
        ),
        currencySelectionUseCases = CurrencySelectionUseCases(
            getCalculatorSelection = mockk<GetCalculatorSelectionUseCase>(),
            saveCalculatorMainCurrency = mockk<SaveCalculatorMainCurrencyUseCase>(),
            saveCalculatorSubCurrency = mockk<SaveCalculatorSubCurrencyUseCase>(),
            saveCalculatorSelection = mockk<SaveCalculatorSelectionUseCase>(),
            getExchangeSelection = getExchangeSelectionUseCase,
            saveExchangeFromCurrency = mockk<SaveExchangeFromCurrencyUseCase>(),
            saveExchangeToCurrency = saveExchangeToCurrencyUseCase,
            saveExchangeSelection = mockk<SaveExchangeSelectionUseCase>(),
        ),
    )

    val originalLocale = Locale.getDefault()

    beforeEach {
        clearAllMocks()
        Locale.setDefault(Locale.KOREA)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        coEvery { getSupportedCurrenciesUseCase() } returns mockCurrencies
        coEvery { getLatestExchangeRateDateUseCase() } returns ""
        coEvery { getLatestExchangeRateFetchedAtUseCase() } returns 0L
        coEvery { getExchangeSelectionUseCase() } returns ExchangeCurrencySelection(
            fromCode = null,
            toCode = null,
        )
        coEvery { saveExchangeToCurrencyUseCase(any()) } returns Unit
        every { getFavoriteCurrenciesUseCase() } returns MutableStateFlow(emptyList())
        every { calculateExchangeAmountUseCase(any(), any()) } returns CONVERTED_AMOUNT
        coEvery { getExchangeRateUseCase("USD", "KRW") } coAnswers {
            delay(SLOW_RESPONSE_MILLIS)
            throw ExchangeRateException.TemporarilyUnavailable()
        }
        coEvery { getExchangeRateUseCase("USD", "JPY") } returns JPY_RATE
    }
    afterEach {
        Dispatchers.resetMain()
        Locale.setDefault(originalLocale)
    }

    Given("첫 통화쌍의 환율 조회가 아직 끝나지 않은 환율 화면에서") {
        When("사용자가 받을 통화를 바꿔 새 통화쌍이 먼저 성공한 뒤 이전 조회가 실패하면") {
            Then("이전 실패는 반영되지 않고 바뀐 통화쌍의 환율이 남아야 한다") {
                runTest(testDispatcher) {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()
                        runCurrent()

                        viewModel.container.stateFlow.value.toCurrency shouldBe mockCurrencies[1]
                        viewModel.container.stateFlow.value.isLoading shouldBe true

                        viewModel.processIntent(ExchangeContract.Intent.SelectToCurrency(jpy))
                        runCurrent()

                        viewModel.container.stateFlow.value.toCurrency shouldBe jpy
                        viewModel.container.stateFlow.value.exchangeRate shouldBe JPY_RATE
                        viewModel.container.stateFlow.value.failure shouldBe null

                        advanceTimeBy(SLOW_RESPONSE_MILLIS)
                        runCurrent()

                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "KRW") }
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }
                        viewModel.container.stateFlow.value.failure shouldBe null
                        viewModel.container.stateFlow.value.exchangeRate shouldBe JPY_RATE
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("이전 통화쌍 조회가 실패해도 새 조회가 아직 끝나지 않았을 때") {
        When("이전 조회의 실패 응답이 먼저 도착하면") {
            Then("진행 중인 조회의 로딩 표시가 꺼지지 않아야 한다") {
                runTest(testDispatcher) {
                    coEvery { getExchangeRateUseCase("USD", "JPY") } coAnswers {
                        delay(SLOW_RESPONSE_MILLIS * 2)
                        JPY_RATE
                    }
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()
                        runCurrent()

                        viewModel.processIntent(ExchangeContract.Intent.SelectToCurrency(jpy))
                        runCurrent()

                        viewModel.container.stateFlow.value.isLoading shouldBe true

                        advanceTimeBy(SLOW_RESPONSE_MILLIS)
                        runCurrent()

                        viewModel.container.stateFlow.value.isLoading shouldBe true
                        viewModel.container.stateFlow.value.failure shouldBe null

                        advanceTimeBy(SLOW_RESPONSE_MILLIS)
                        runCurrent()

                        viewModel.container.stateFlow.value.isLoading shouldBe false
                        viewModel.container.stateFlow.value.exchangeRate shouldBe JPY_RATE
                        viewModel.container.stateFlow.value.failure shouldBe null
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("환율 조회가 느리게 실패하는 환율 화면에서") {
        When("사용자가 통화를 바꾸지 않은 채 그 조회가 실패하면") {
            Then("요청한 통화쌍 그대로이므로 실패 상태가 되어야 한다") {
                runTest(testDispatcher) {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()
                        runCurrent()

                        viewModel.container.stateFlow.value.isLoading shouldBe true
                        viewModel.container.stateFlow.value.failure shouldBe null

                        advanceTimeBy(SLOW_RESPONSE_MILLIS)
                        runCurrent()

                        viewModel.container.stateFlow.value.isLoading shouldBe false
                        viewModel.container.stateFlow.value.failure shouldBe rateFailure
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }
})

private const val SLOW_RESPONSE_MILLIS = 100L
private const val JPY_RATE = 9.0
private const val CONVERTED_AMOUNT = "0"
