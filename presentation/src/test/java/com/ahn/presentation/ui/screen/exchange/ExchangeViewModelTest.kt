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
import com.ahn.domain.exchange.usecase.CalculateExchangeAmountUseCase
import com.ahn.domain.exchange.usecase.ConvertExchangeAmountUseCase
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.exchange.usecase.GetExchangeRateUseCase
import com.ahn.domain.exchange.usecase.GetSupportedCurrenciesUseCase
import com.ahn.domain.favorite.usecase.BuildFavoriteRatesUseCase
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.domain.favorite.usecase.GetFavoriteCurrenciesUseCase
import com.ahn.domain.favorite.usecase.ToggleFavoriteCurrencyUseCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModelTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val testDispatcher = UnconfinedTestDispatcher()

    val mockCurrencies = listOf(
        CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸"),
        CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷"),
    )
    val usd = mockCurrencies[0]
    val krw = mockCurrencies[1]

    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()
    val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
    val getFavoriteCurrenciesUseCase = mockk<GetFavoriteCurrenciesUseCase>()
    val toggleFavoriteCurrencyUseCase = mockk<ToggleFavoriteCurrencyUseCase>()
    val calculateExchangeAmountUseCase = mockk<CalculateExchangeAmountUseCase>()
    val convertExchangeAmountUseCase = mockk<ConvertExchangeAmountUseCase>()
    val buildFavoriteRatesUseCase = mockk<BuildFavoriteRatesUseCase>()
    val getCalculatorSelectionUseCase = mockk<GetCalculatorSelectionUseCase>()
    val saveCalculatorMainCurrencyUseCase = mockk<SaveCalculatorMainCurrencyUseCase>()
    val saveCalculatorSubCurrencyUseCase = mockk<SaveCalculatorSubCurrencyUseCase>()
    val saveCalculatorSelectionUseCase = mockk<SaveCalculatorSelectionUseCase>()
    val getExchangeSelectionUseCase = mockk<GetExchangeSelectionUseCase>()
    val saveExchangeFromCurrencyUseCase = mockk<SaveExchangeFromCurrencyUseCase>()
    val saveExchangeToCurrencyUseCase = mockk<SaveExchangeToCurrencyUseCase>()
    val saveExchangeSelectionUseCase = mockk<SaveExchangeSelectionUseCase>()

    fun createViewModel() = ExchangeViewModel(
        exchangeUseCases = ExchangeUseCases(
            exchangeAmount = calculateExchangeAmountUseCase,
            convertExchangeAmount = convertExchangeAmountUseCase,
            getExchangeRate = getExchangeRateUseCase,
            getSupportedCurrencies = getSupportedCurrenciesUseCase,
        ),
        favoriteUseCases = FavoriteUseCases(
            buildFavoriteRates = buildFavoriteRatesUseCase,
            getFavoriteCurrencies = getFavoriteCurrenciesUseCase,
            toggleFavoriteCurrency = toggleFavoriteCurrencyUseCase,
        ),
        currencySelectionUseCases = CurrencySelectionUseCases(
            getCalculatorSelection = getCalculatorSelectionUseCase,
            saveCalculatorMainCurrency = saveCalculatorMainCurrencyUseCase,
            saveCalculatorSubCurrency = saveCalculatorSubCurrencyUseCase,
            saveCalculatorSelection = saveCalculatorSelectionUseCase,
            getExchangeSelection = getExchangeSelectionUseCase,
            saveExchangeFromCurrency = saveExchangeFromCurrencyUseCase,
            saveExchangeToCurrency = saveExchangeToCurrencyUseCase,
            saveExchangeSelection = saveExchangeSelectionUseCase,
        ),
    )

    beforeEach {
        Dispatchers.setMain(testDispatcher)
        coEvery { getSupportedCurrenciesUseCase() } returns mockCurrencies
        coEvery { getExchangeRateUseCase(any(), any()) } returns 1500.00
        every { getFavoriteCurrenciesUseCase() } returns MutableStateFlow(emptyList())
        coEvery { toggleFavoriteCurrencyUseCase(any()) } returns Unit
        every { calculateExchangeAmountUseCase(any(), any()) } returns "1500.00"
        coEvery { getExchangeSelectionUseCase() } returns ExchangeCurrencySelection(
            fromCode = null,
            toCode = null,
        )
        coEvery { saveExchangeFromCurrencyUseCase(any()) } returns Unit
        coEvery { saveExchangeToCurrencyUseCase(any()) } returns Unit
        coEvery { saveExchangeSelectionUseCase(any(), any()) } returns Unit
    }
    afterEach { Dispatchers.resetMain() }

    Given("ViewModel이 처음 생성되었을 때") {
        When("초기 상태를 확인하면") {
            Then("초기화 과정이 순차적으로 올바르게 진행되어야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        runOnCreate() // 초기화 시작

                        // performLoadCurrencies() 완료 후 observeFavorites() 호출 → 첫 emission은 로딩 ON
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }

                        // ─── [2단계] 자동으로 이어서 실행된 performFetchExchangeRate() 구간 ───

                        // 1. 환율 API 호출 직전 로딩 켜짐
                        expectState { copy(isLoading = true) }

                        // 2. 환율 데이터 수신 후 계산 완료, 로딩 꺼짐
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00" // 기본 금액 "1" * 1500.00
                            )
                        }
                        // observeFavorites() 무한 collect 때문에 joinIntents가 끝나지 않음 → 컨테이너 취소 필요
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("저장된 환율 화면 통화 코드가 있으면") {
            Then("저장된 from/to 통화로 초기화되어야 한다") {
                runTest {
                    coEvery { getExchangeSelectionUseCase() } returns ExchangeCurrencySelection(
                        fromCode = "KRW",
                        toCode = "USD",
                    )

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()

                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = krw,
                                toCurrency = usd,
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00",
                            )
                        }

                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("환율 화면에서 통화를 선택할 때") {
        When("from 통화를 변경하면") {
            Then("선택한 from 통화 코드가 저장되어야 한다") {
                runTest {
                    val jpy = CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵")
                    val currencies = mockCurrencies + jpy

                    coEvery { getSupportedCurrenciesUseCase() } returns currencies
                    coEvery { getExchangeRateUseCase("JPY", "KRW") } returns 9.0

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()

                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = currencies,
                                fromCurrency = usd,
                                toCurrency = krw,
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00",
                            )
                        }

                        containerHost.processIntent(ExchangeContract.Intent.SelectFromCurrency(jpy))

                        expectState { copy(fromCurrency = jpy) }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 9.0,
                                toAmount = "1500.00",
                            )
                        }

                        coVerify(exactly = 1) {
                            saveExchangeFromCurrencyUseCase("JPY")
                        }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("to 통화를 변경하면") {
            Then("선택한 to 통화 코드가 저장되어야 한다") {
                runTest {
                    val jpy = CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵")
                    val currencies = mockCurrencies + jpy

                    coEvery { getSupportedCurrenciesUseCase() } returns currencies
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 150.0

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()

                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = currencies,
                                fromCurrency = usd,
                                toCurrency = krw,
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00",
                            )
                        }

                        containerHost.processIntent(ExchangeContract.Intent.SelectToCurrency(jpy))

                        expectState { copy(toCurrency = jpy) }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 150.0,
                                toAmount = "1500.00",
                            )
                        }

                        coVerify(exactly = 1) {
                            saveExchangeToCurrencyUseCase("JPY")
                        }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("통화를 스왑하면") {
            Then("스왑된 from/to 통화 코드가 저장되어야 한다") {
                runTest {
                    coEvery { getExchangeRateUseCase("KRW", "USD") } returns 0.001

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()

                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw,
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00",
                            )
                        }

                        containerHost.processIntent(ExchangeContract.Intent.SwapCurrencies)

                        expectState {
                            copy(
                                fromCurrency = krw,
                                toCurrency = usd,
                                fromAmount = "1500.00",
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 0.001,
                                toAmount = "1500.00",
                            )
                        }

                        coVerify(exactly = 1) {
                            saveExchangeSelectionUseCase("KRW", "USD")
                        }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("금액을 입력할 때") {
        When("유효한 금액을 입력하면") {
            Then("입력값과 환전 금액이 반영되어야 한다") {
                runTest {
                    every { calculateExchangeAmountUseCase("1000", 1500.00) } returns "1500000.00"

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        // 1. 초기화 시작
                        runOnCreate()


                        expectState { copy(isLoading = true) }

                        // 통화 목록 세팅 완료, 로딩 꺼짐
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }

                        expectState { copy(isLoading = true) }

                        // 환율 세팅 완료, 로딩 꺼짐, 기본 계산(1 * 1500.00) 세팅
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00"
                            )
                        }
                        // ────────────────────────────────

                        // 2. 이제 큐가 깨끗해졌으니 사용자 액션 실행
                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("1000"))

                        // 3. 액션에 대한 결과 검증
                        expectState {
                            copy(
                                fromAmount = "1000",
                                toAmount = "1500000.00"
                            )
                        }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("통화 목록 로드가 실패할 때") {
        When("ViewModel이 생성되면") {
            Then("에러 스낵바 SideEffect가 발생해야 한다") {
                runTest {
                    // 테스트가 본격적으로 시작되는 여기서 예외를 던지도록 덮어씌움
                    coEvery { getSupportedCurrenciesUseCase() } throws Exception("Network Error")

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        runOnCreate()

                        expectState { copy(isLoading = true) }

                        // 에러가 발생하여 캐치(catch) 블록으로 이동, 로딩만 꺼지고 상태는 그대로
                        expectState { copy(isLoading = false) }

                        // 4. 에러 스낵바 SideEffect 발생 검증
                        expectSideEffect(
                            ExchangeContract.SideEffect.ShowSnackBar(UiText.StringResource(R.string.load_currency_list_failed, listOf("Network Error")))
                        )
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("환율 API 호출이 실패할 때") {
        When("ViewModel이 생성되면") {
            Then("에러 스낵바 SideEffect가 발생해야 한다") {
                runTest {
                    // 환율 API만 실패하도록 덮어씨움
                    coEvery { getExchangeRateUseCase(any(), any()) } throws Exception("Network Error")

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        runOnCreate()

                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }

                        // ─── [3단계: 환율 로드 (여기서 실패)] ───
                        expectState { copy(isLoading = true) }
                        expectState { copy(isLoading = false) } // 실패해서 로딩만 꺼짐

                        // SideEffect 검증
                        expectSideEffect(
                            ExchangeContract.SideEffect.ShowSnackBar(UiText.StringResource(R.string.load_exchange_rate_failed, listOf("Network Error")))
                        )
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }

    Given("즐겨찾기를 토글할 때") {
        When("즐겨찾기에 없는 통화(USD)를 하트 누르면") {
            Then("즐겨찾기에 추가되어 상태에 반영되어야 한다") {
                runTest {
                    val favoritesFlow = MutableStateFlow<List<String>>(emptyList())

                    every { getFavoriteCurrenciesUseCase() } returns favoritesFlow
                    coEvery { toggleFavoriteCurrencyUseCase("USD") } coAnswers {
                        favoritesFlow.value = listOf("USD")
                    }

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()

                        // 초기 로딩 상태 소비
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00"
                            )
                        }

                        containerHost.processIntent(ExchangeContract.Intent.ToggleFavorite("USD"))

                        // postSideEffect가 Flow collect의 reduce보다 먼저 스트림에 올 수 있음
                        expectSideEffect(
                            ExchangeContract.SideEffect.ShowSnackBar(UiText.StringResource(R.string.favorite_added))
                        )
                        expectState {
                            copy(favoriteCurrencyCodes = listOf("USD"))
                        }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("이미 즐겨찾기인 통화(USD)를 다시 하트 누르면") {
            Then("즐겨찾기에서 제거되어 상태에 반영되어야 한다") {
                runTest {
                    val favoritesFlow = MutableStateFlow(listOf("USD"))

                    coEvery { getSupportedCurrenciesUseCase() } returns mockCurrencies
                    coEvery { getExchangeRateUseCase(any(), any()) } returns 1500.00
                    every { getFavoriteCurrenciesUseCase() } returns favoritesFlow
                    coEvery { toggleFavoriteCurrencyUseCase("USD") } coAnswers {
                        favoritesFlow.value = emptyList()
                    }

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        runOnCreate()

                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1500.00,
                                toAmount = "1500.00"
                            )
                        }
                        // performLoadCurrencies + 환율 완료 후 observeFavorites()에서 [USD] 반영
                        expectState { copy(favoriteCurrencyCodes = listOf("USD")) }

                        containerHost.processIntent(ExchangeContract.Intent.ToggleFavorite("USD"))

                        expectSideEffect(
                            ExchangeContract.SideEffect.ShowSnackBar(UiText.StringResource(R.string.favorite_removed))
                        )
                        expectState { copy(favoriteCurrencyCodes = emptyList()) }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }
})
