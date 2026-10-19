package com.ahn.presentation.ui.screen.calculator

import android.util.Log
import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import com.ahn.domain.calculator.usecase.CalculatorUseCases
import com.ahn.domain.calculator.usecase.ExtractRepeatOperationUseCase
import com.ahn.domain.calculator.usecase.GetCalculatorHistoriesUseCase
import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.usecase.CurrencySelectionUseCases
import com.ahn.domain.currency.usecase.GetCalculatorSelectionUseCase
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.exchange.usecase.GetSupportedCurrenciesUseCase
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.domain.favorite.usecase.GetFavoriteCurrenciesUseCase
import com.ahn.domain.review.usecase.RecordCalculationSuccessUseCase
import com.ahn.domain.review.usecase.ReviewPromptUseCases
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.OrbitTestContext
import org.orbitmvi.orbit.test.test

/**
 * 계산기에서 인앱 리뷰를 요청하는 흐름만 본다. 나머지 계산기 동작은 [CalculatorViewModelTest]가 맡는다.
 *
 * 이 흐름에 쓰이지 않는 UseCase는 relaxed mock으로 둔다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelReviewPromptTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val testDispatcher = UnconfinedTestDispatcher()
        val calculateExpressionUseCase = mockk<CalculateExpressionUseCase>()
        val getCalculatorHistoriesUseCase = mockk<GetCalculatorHistoriesUseCase>()
        val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
        val getFavoriteCurrenciesUseCase = mockk<GetFavoriteCurrenciesUseCase>()
        val getCalculatorSelectionUseCase = mockk<GetCalculatorSelectionUseCase>()
        val recordCalculationSuccessUseCase = mockk<RecordCalculationSuccessUseCase>()

        fun createViewModel() = CalculatorViewModel(
            calculatorUseCases = CalculatorUseCases(
                addHistory = mockk(relaxed = true),
                calculateExpression = calculateExpressionUseCase,
                clearHistory = mockk(relaxed = true),
                extractRepeatOperation = ExtractRepeatOperationUseCase(),
                getHistory = getCalculatorHistoriesUseCase,
            ),
            exchangeUseCases = ExchangeUseCases(
                exchangeAmount = mockk(relaxed = true),
                convertExchangeAmount = mockk(relaxed = true),
                getExchangeRate = mockk(relaxed = true),
                getLatestRateDate = mockk(relaxed = true),
                getLatestFetchedAt = mockk(relaxed = true),
                refreshExchangeRates = mockk(relaxed = true),
                getSupportedCurrencies = getSupportedCurrenciesUseCase,
            ),
            favoriteUseCases = FavoriteUseCases(
                buildFavoriteRates = mockk(relaxed = true),
                getFavoriteCurrencies = getFavoriteCurrenciesUseCase,
                toggleFavoriteCurrency = mockk(relaxed = true),
            ),
            currencySelectionUseCases = CurrencySelectionUseCases(
                getCalculatorSelection = getCalculatorSelectionUseCase,
                saveCalculatorMainCurrency = mockk(relaxed = true),
                saveCalculatorSubCurrency = mockk(relaxed = true),
                saveCalculatorSelection = mockk(relaxed = true),
                getExchangeSelection = mockk(relaxed = true),
                saveExchangeFromCurrency = mockk(relaxed = true),
                saveExchangeToCurrency = mockk(relaxed = true),
                saveExchangeSelection = mockk(relaxed = true),
            ),
            reviewPromptUseCases = ReviewPromptUseCases(
                recordAppOpen = mockk(relaxed = true),
                recordCalculationSuccess = recordCalculationSuccessUseCase,
            ),
        )

        /** "1+1"을 입력하고 =을 눌러 "2"가 될 때까지의 상태를 소비한다. */
        suspend fun OrbitTestContext<
            CalculatorContract.State,
            CalculatorContract.SideEffect,
            CalculatorViewModel,
        >.calculateOnePlusOne() {
            expectInitialState()

            containerHost.processIntent(CalculatorContract.Intent.Input(CalculatorToken.Number("1")))
            expectState { copy(expression = "1", cursorPosition = 1) }

            containerHost.processIntent(CalculatorContract.Intent.Input(CalculatorToken.Operator("+")))
            expectState { copy(expression = "1+", cursorPosition = 2) }

            containerHost.processIntent(CalculatorContract.Intent.Input(CalculatorToken.Number("1")))
            expectState { copy(expression = "1+1", cursorPosition = 3, previewResult = "2") }

            containerHost.processIntent(CalculatorContract.Intent.Calculate)
            expectState {
                copy(
                    expression = "2",
                    cursorPosition = 1,
                    previewResult = "",
                    repeatOperation = "+1",
                    isCalculatedResult = true,
                    histories = listOf(CalculatorContract.HistoryItem(expression = "1+1", result = "2")),
                )
            }
        }

        beforeSpec { Dispatchers.setMain(testDispatcher) }
        afterSpec { Dispatchers.resetMain() }

        beforeEach {
            clearAllMocks()

            every { calculateExpressionUseCase.calculate(any()) } returns "0"
            every { calculateExpressionUseCase.calculate("1+1") } returns "2"
            every { getCalculatorHistoriesUseCase() } returns MutableStateFlow(emptyList())
            coEvery { getSupportedCurrenciesUseCase() } returns emptyList()
            every { getFavoriteCurrenciesUseCase() } returns MutableStateFlow(emptyList())
            coEvery { getCalculatorSelectionUseCase() } returns CalculatorCurrencySelection(
                mainCode = null,
                subCode = null,
            )
            coEvery { recordCalculationSuccessUseCase() } returns false
        }

        Given("리뷰 요청 조건을 만족한 상태에서") {
            When("계산에 성공하면") {
                Then("결과를 반영한 뒤 리뷰 요청 SideEffect가 발생해야 한다") {
                    runTest {
                        coEvery { recordCalculationSuccessUseCase() } returns true

                        createViewModel().test(this) {
                            calculateOnePlusOne()

                            expectSideEffect(CalculatorContract.SideEffect.RequestReview)
                        }

                        coVerify(exactly = 1) { recordCalculationSuccessUseCase() }
                    }
                }
            }
        }

        Given("리뷰 요청 조건을 만족하지 못한 상태에서") {
            When("계산에 성공하면") {
                Then("계산 성공은 기록하되 리뷰 요청 SideEffect는 없어야 한다") {
                    runTest {
                        // 남은 SideEffect가 있으면 orbit-test가 실패시킨다.
                        createViewModel().test(this) {
                            calculateOnePlusOne()
                        }

                        coVerify(exactly = 1) { recordCalculationSuccessUseCase() }
                    }
                }
            }
        }

        Given("계산 결과가 에러일 때") {
            When("=을 누르면") {
                Then("실패한 계산은 리뷰 요청 조건에 세지 않아야 한다") {
                    runTest {
                        every { calculateExpressionUseCase.calculate(any()) } returns "Error"

                        createViewModel().test(this) {
                            expectInitialState()

                            containerHost.processIntent(CalculatorContract.Intent.Input(CalculatorToken.Number("1")))
                            expectState { copy(expression = "1", cursorPosition = 1) }

                            containerHost.processIntent(CalculatorContract.Intent.Input(CalculatorToken.Operator("+")))
                            expectState { copy(expression = "1+", cursorPosition = 2) }

                            containerHost.processIntent(CalculatorContract.Intent.Calculate)
                            expectState {
                                copy(isError = true, errorMessage = UiText.StringResource(R.string.calculator_error))
                            }
                            expectSideEffect(
                                CalculatorContract.SideEffect.ShowSnackBar(
                                    UiText.StringResource(R.string.invalid_expression),
                                ),
                            )
                        }

                        coVerify(exactly = 0) { recordCalculationSuccessUseCase() }
                    }
                }
            }
        }

        Given("리뷰 요청 기록 저장이 실패하는 상태에서") {
            When("계산에 성공하면") {
                Then("계산 결과는 그대로 반영되고 리뷰 요청 SideEffect는 없어야 한다") {
                    runTest {
                        mockkStatic(Log::class)
                        try {
                            every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
                            coEvery { recordCalculationSuccessUseCase() } throws IllegalStateException("save failed")

                            createViewModel().test(this) {
                                calculateOnePlusOne()
                            }

                            verify(exactly = 1) {
                                Log.w(
                                    "CalculatorViewModel",
                                    "Failed to record calculation for review prompt.",
                                    any<Throwable>(),
                                )
                            }
                        } finally {
                            unmockkStatic(Log::class)
                        }
                    }
                }
            }
        }
    })
