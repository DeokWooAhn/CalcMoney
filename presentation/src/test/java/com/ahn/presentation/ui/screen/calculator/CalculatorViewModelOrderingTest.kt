package com.ahn.presentation.ui.screen.calculator

import android.util.Log
import com.ahn.domain.ads.usecase.InterstitialAdUseCases
import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.usecase.AddCalculatorHistoryUseCase
import com.ahn.domain.calculator.usecase.CalculateExpressionUseCase
import com.ahn.domain.calculator.usecase.CalculatorUseCases
import com.ahn.domain.calculator.usecase.ClearCalculatorHistoriesUseCase
import com.ahn.domain.calculator.usecase.ExtractRepeatOperationUseCase
import com.ahn.domain.calculator.usecase.GetCalculatorHistoriesUseCase
import com.ahn.domain.currency.model.CalculatorCurrencySelection
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.currency.usecase.CurrencySelectionUseCases
import com.ahn.domain.currency.usecase.GetCalculatorSelectionUseCase
import com.ahn.domain.currency.usecase.SaveCalculatorMainCurrencyUseCase
import com.ahn.domain.exchange.model.ExchangeRateException
import com.ahn.domain.exchange.usecase.ConvertExchangeAmountUseCase
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.exchange.usecase.GetExchangeRateUseCase
import com.ahn.domain.exchange.usecase.GetSupportedCurrenciesUseCase
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import com.ahn.domain.favorite.usecase.GetFavoriteCurrenciesUseCase
import com.ahn.domain.review.usecase.ReviewPromptUseCases
import com.ahn.presentation.R
import com.ahn.presentation.util.UiText
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitTestContext
import org.orbitmvi.orbit.test.test

/**
 * 인텐트가 겹칠 때 계산기가 지켜야 할 순서를 본다. iOS `CalculatorViewModelOrderingTests`와 같은 시나리오다.
 *
 * 저장·조회에 지연을 넣어, 한 인텐트가 기다리는 사이 다음 인텐트가 끼어드는 상황을 만든다.
 * 지연은 가상 시간이라 실제로 기다리지 않는다. 시나리오와 원칙은 `docs/decisions/calculator-intent-ordering.md`에 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelOrderingTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val testDispatcher = UnconfinedTestDispatcher()
        val krw = CurrencyInfo(code = "KRW", displayCode = "KRW", name = "원", flagEmoji = "🇰🇷")
        val usd = CurrencyInfo(code = "USD", displayCode = "USD", name = "달러", flagEmoji = "🇺🇸")
        val eur = CurrencyInfo(code = "EUR", displayCode = "EUR", name = "유로", flagEmoji = "🇪🇺")
        val jpy = CurrencyInfo(code = "JPY", displayCode = "JPY", name = "엔", flagEmoji = "🇯🇵")

        val addHistoryUseCase = mockk<AddCalculatorHistoryUseCase>()
        val clearHistoryUseCase = mockk<ClearCalculatorHistoriesUseCase>()
        val getHistoryUseCase = mockk<GetCalculatorHistoriesUseCase>()
        val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
        val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()
        val getCalculatorSelectionUseCase = mockk<GetCalculatorSelectionUseCase>()
        val saveMainCurrencyUseCase = mockk<SaveCalculatorMainCurrencyUseCase>()
        val getFavoriteCurrenciesUseCase = mockk<GetFavoriteCurrenciesUseCase>()

        /** 저장이 끝난 순서. 기록 추가는 `add:<수식>`, 기록 삭제는 `clear`로 남긴다. */
        val savedOperations = mutableListOf<String>()
        val savedMainCurrencyCodes = mutableListOf<String>()
        val requestedRatePairs = mutableListOf<String>()

        /** 응답(성공·실패)까지 끝난 환율 조회. 취소된 요청도 실제 네트워크처럼 끝까지 응답한다. */
        val completedRatePairs = mutableListOf<String>()

        /** 저장소의 기록 흐름. 기본은 빈 목록이고, 시나리오가 저장 중에 값을 바꿔 늦게 도착한 기록을 흉내 낸다. */
        val historyFlow = MutableStateFlow<List<CalculatorHistory>>(emptyList())

        var addHistoryDelayMillis = 0L
        var addHistoryError: Throwable? = null
        var supportedCurrenciesDelayMillis = 0L
        val mainCurrencySaveDelayMillis = mutableMapOf<String, Long>()

        /** 통화쌍별로 몇 번째 조회에 어떻게 응답할지. 적어 두지 않은 조회는 바로 1.0을 돌려준다. */
        val rateScript = mutableMapOf<String, ArrayDeque<RateStep>>()

        fun createViewModel(): CalculatorViewModel {
            val calculateExpression = CalculateExpressionUseCase()

            return CalculatorViewModel(
                calculatorUseCases = CalculatorUseCases(
                    addHistory = addHistoryUseCase,
                    calculateExpression = calculateExpression,
                    clearHistory = clearHistoryUseCase,
                    extractRepeatOperation = ExtractRepeatOperationUseCase(),
                    getHistory = getHistoryUseCase,
                ),
                exchangeUseCases = ExchangeUseCases(
                    exchangeAmount = mockk(relaxed = true),
                    convertExchangeAmount = ConvertExchangeAmountUseCase(calculateExpression),
                    getExchangeRate = getExchangeRateUseCase,
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
                    saveCalculatorMainCurrency = saveMainCurrencyUseCase,
                    saveCalculatorSubCurrency = mockk(relaxed = true),
                    saveCalculatorSelection = mockk(relaxed = true),
                    getExchangeSelection = mockk(relaxed = true),
                    saveExchangeFromCurrency = mockk(relaxed = true),
                    saveExchangeToCurrency = mockk(relaxed = true),
                    saveExchangeSelection = mockk(relaxed = true),
                ),
                reviewPromptUseCases = ReviewPromptUseCases(
                    recordAppOpen = mockk(relaxed = true),
                    recordCalculationSuccess = mockk(relaxed = true),
                ),
                interstitialAdUseCases = InterstitialAdUseCases(
                    recordCalculation = mockk(relaxed = true),
                    shouldShow = mockk(relaxed = true),
                    recordShown = mockk(relaxed = true),
                ),
            )
        }

        /**
         * 시작 로드를 돌리고, 시나리오를 실행한 뒤 남은 작업을 모두 끝낸다.
         *
         * 마지막 상태와 그동안 나온 SideEffect를 [verify]에 넘긴다. 상태는 순서를 따지기 위해 중간값이 아니라 마지막 값만 본다.
         */
        suspend fun TestScope.runScenario(
            finishInitialLoad: Boolean = true,
            scenario: suspend OrbitTestContext<
                CalculatorContract.State,
                CalculatorContract.SideEffect,
                CalculatorViewModel,
            >.(TestScope) -> Unit,
            verify: (CalculatorContract.State, List<CalculatorContract.SideEffect>) -> Unit,
        ) {
            val testScope = this
            createViewModel().test(this, timeout = DRAIN_TIMEOUT) {
                runOnCreate()
                if (finishInitialLoad) testScope.settle()

                scenario(testScope)

                val finalState = containerHost.container.stateFlow.value
                val sideEffects = drainSideEffects()
                cancelAndIgnoreRemainingItems()

                verify(finalState, sideEffects)
            }
        }

        beforeSpec { Dispatchers.setMain(testDispatcher) }
        afterSpec { Dispatchers.resetMain() }

        beforeEach {
            clearAllMocks()
            savedOperations.clear()
            savedMainCurrencyCodes.clear()
            requestedRatePairs.clear()
            completedRatePairs.clear()
            historyFlow.value = emptyList()
            mainCurrencySaveDelayMillis.clear()
            rateScript.clear()
            addHistoryDelayMillis = 0L
            addHistoryError = null
            supportedCurrenciesDelayMillis = 0L

            every { getHistoryUseCase() } returns historyFlow
            every { getFavoriteCurrenciesUseCase() } returns MutableStateFlow(emptyList())
            coEvery { getCalculatorSelectionUseCase() } returns CalculatorCurrencySelection(
                mainCode = "KRW",
                subCode = "JPY",
            )
            coEvery { getSupportedCurrenciesUseCase() } coAnswers {
                delay(supportedCurrenciesDelayMillis)
                listOf(krw, usd, eur, jpy)
            }
            coEvery { addHistoryUseCase(any()) } coAnswers {
                delay(addHistoryDelayMillis)
                addHistoryError?.let { throw it }
                savedOperations += "add:${firstArg<CalculatorHistory>().expression}"
            }
            coEvery { clearHistoryUseCase() } coAnswers {
                savedOperations += "clear"
            }
            coEvery { saveMainCurrencyUseCase(any()) } coAnswers {
                val code = firstArg<String>()
                delay(mainCurrencySaveDelayMillis[code] ?: 0L)
                savedMainCurrencyCodes += code
            }
            coEvery { getExchangeRateUseCase(any(), any()) } coAnswers {
                val pair = "${firstArg<String>()}->${secondArg<String>()}"
                requestedRatePairs += pair
                val step = rateScript[pair]?.removeFirstOrNull() ?: RateStep()
                // 실제 Firestore 요청은 호출한 쪽이 취소해도 멈추지 않는다. 취소와 무관하게 응답이 돌아오게 해
                // 요청 번호 확인만으로 지난 응답을 버리는지 본다.
                withContext(NonCancellable) { delay(step.delayMillis) }
                completedRatePairs += pair
                step.error?.let { throw it }
                step.rate
            }
        }

        Given("1. 기록 저장이 늦게 끝나는 상태에서") {
            When("1+2를 입력하고 = 를 연달아 누르면") {
                Then("기록은 1+2, 3+2 순서로 저장되고 화면은 5여야 한다") {
                    runTest {
                        addHistoryDelayMillis = 20L

                        runScenario(
                            scenario = { testScope ->
                                containerHost.inputOnePlusTwo()
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                testScope.settle()
                            },
                            verify = { state, _ ->
                                savedOperations shouldBe listOf("add:1+2", "add:3+2")
                                state.expression shouldBe "5"
                            },
                        )
                    }
                }
            }
        }

        Given("2. 기록 저장이 늦게 끝나는 상태에서") {
            When("= 를 누르고 기록을 지운 뒤 다시 = 를 누르면") {
                Then("추가·삭제·추가 순서로 저장되고 남는 기록은 3+2 하나여야 한다") {
                    runTest {
                        addHistoryDelayMillis = 20L

                        runScenario(
                            scenario = { testScope ->
                                containerHost.inputOnePlusTwo()
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                containerHost.processIntent(CalculatorContract.Intent.ClearHistory)
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                testScope.settle()
                            },
                            verify = { state, _ ->
                                savedOperations shouldBe listOf("add:1+2", "clear", "add:3+2")
                                state.histories shouldBe listOf(
                                    CalculatorContract.HistoryItem(expression = "3+2", result = "5"),
                                )
                            },
                        )
                    }
                }
            }
        }

        Given("3. 먼저 고른 통화의 저장이 나중에 고른 통화보다 늦게 끝나는 상태에서") {
            When("메인 통화를 USD, EUR 순서로 빠르게 고르면") {
                Then("USD, EUR 순서로 저장되고 화면에는 EUR이 남아야 한다") {
                    runTest {
                        mainCurrencySaveDelayMillis["USD"] = 60L
                        mainCurrencySaveDelayMillis["EUR"] = 5L

                        runScenario(
                            scenario = { testScope ->
                                containerHost.processIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(usd))
                                containerHost.processIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(eur))
                                testScope.settle()
                            },
                            verify = { state, _ ->
                                savedMainCurrencyCodes shouldBe listOf("USD", "EUR")
                                state.mainExchangeCurrency?.code shouldBe "EUR"
                            },
                        )
                    }
                }
            }
        }

        Given("4-1. 첫 USD 환율 응답이 가장 늦게 오는 상태에서") {
            When("메인 통화를 USD, EUR, USD 순서로 고르면") {
                Then("마지막 요청의 환율만 남아야 한다") {
                    runTest {
                        rateScript["USD->JPY"] = ArrayDeque(
                            listOf(
                                RateStep(delayMillis = 100L, rate = 1.5),
                                RateStep(rate = 3.0),
                            ),
                        )
                        rateScript["EUR->JPY"] = ArrayDeque(listOf(RateStep(rate = 2.0)))

                        runScenario(
                            scenario = { testScope -> selectUsdEurUsd(usd, eur, testScope) },
                            verify = { state, _ ->
                                // 첫 USD 요청도 응답까지 끝났다. 그래도 마지막 요청의 환율이 남아야 한다.
                                completedRatePairs.count { it == "USD->JPY" } shouldBe 2
                                state.mainExchangeCurrency?.code shouldBe "USD"
                                state.exchangeRate shouldBe 3.0
                            },
                        )
                    }
                }
            }
        }

        Given("4-2. 첫 USD 환율 요청이 가장 늦게 실패하는 상태에서") {
            When("메인 통화를 USD, EUR, USD 순서로 고르면") {
                Then("지난 요청의 오류는 스낵바로 띄우지 않아야 한다") {
                    runTest {
                        rateScript["USD->JPY"] = ArrayDeque(
                            listOf(
                                RateStep(delayMillis = 100L, error = ExchangeRateException.NetworkUnavailable()),
                                RateStep(rate = 3.0),
                            ),
                        )
                        rateScript["EUR->JPY"] = ArrayDeque(listOf(RateStep(rate = 2.0)))

                        runScenario(
                            scenario = { testScope -> selectUsdEurUsd(usd, eur, testScope) },
                            verify = { state, sideEffects ->
                                // 첫 USD 요청도 오류 응답까지 끝났다. 그래도 그 오류를 띄우지 않아야 한다.
                                completedRatePairs.count { it == "USD->JPY" } shouldBe 2
                                state.exchangeRate shouldBe 3.0
                                val snackbars =
                                    sideEffects.filterIsInstance<CalculatorContract.SideEffect.ShowSnackBar>()
                                snackbars shouldBe emptyList()
                            },
                        )
                    }
                }
            }
        }

        Given("5. 시작 로드가 끝나지 않은 상태에서") {
            When("1+2를 입력하고 = 를 누르면") {
                Then("로드를 기다리지 않고 결과 3이 바로 나와야 한다") {
                    runTest {
                        supportedCurrenciesDelayMillis = 10_000L

                        runScenario(
                            finishInitialLoad = false,
                            scenario = { testScope ->
                                containerHost.inputOnePlusTwo()
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                // 시간을 흘리지 않고 지금 실행할 수 있는 작업만 돌린다. 로드는 아직 끝나지 않는다.
                                testScope.testScheduler.runCurrent()
                            },
                            verify = { state, _ ->
                                state.expression shouldBe "3"
                                state.isCalculatedResult shouldBe true
                            },
                        )
                    }
                }
            }
        }

        Given("6. 통화를 바꾼 뒤 환율 조회가 끝나지 않은 상태에서") {
            When("1+2를 입력하고 = 를 누르면") {
                Then("조회를 기다리지 않고 결과 3이 바로 나와야 한다") {
                    runTest {
                        rateScript["USD->JPY"] = ArrayDeque(listOf(RateStep(delayMillis = 10_000L)))

                        runScenario(
                            scenario = { testScope ->
                                containerHost.processIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(usd))
                                testScope.testScheduler.runCurrent()
                                requestedRatePairs.last() shouldBe "USD->JPY"

                                containerHost.inputOnePlusTwo()
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                testScope.testScheduler.runCurrent()
                            },
                            verify = { state, _ ->
                                state.expression shouldBe "3"
                                state.isCalculatedResult shouldBe true
                            },
                        )
                    }
                }
            }
        }

        Given("7. 기록 저장이 실패하는 상태에서") {
            When("1+2를 입력하고 = 를 누르면") {
                Then("결과 3은 보이고 기록 목록에는 남지 않으며 저장 실패 스낵바가 떠야 한다") {
                    runTest {
                        mockkStatic(Log::class)
                        try {
                            every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
                            addHistoryError = IllegalStateException("save failed")

                            runScenario(
                                scenario = { testScope ->
                                    containerHost.inputOnePlusTwo()
                                    containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                    testScope.settle()
                                },
                                verify = { state, sideEffects ->
                                    state.expression shouldBe "3"
                                    state.histories shouldBe emptyList()
                                    sideEffects shouldBe listOf(
                                        CalculatorContract.SideEffect.ShowSnackBar(
                                            UiText.StringResource(R.string.calculator_history_save_failed),
                                        ),
                                    )
                                },
                            )
                        } finally {
                            unmockkStatic(Log::class)
                        }
                    }
                }
            }
        }

        Given("8. 기존 기록이 기록 흐름으로 늦게 도착하는 상태에서") {
            When("1+2를 입력하고 = 를 누르면") {
                Then("저장하는 동안 들어온 기존 기록이 화면 목록에서 빠지지 않아야 한다") {
                    runTest {
                        val earlierHistory = CalculatorHistory(expression = "7×6", result = "42")
                        coEvery { addHistoryUseCase(any()) } coAnswers {
                            delay(20L)
                            // 시작 로드가 늦어 화면 목록은 아직 비어 있었다. 저장이 끝나 저장소가 기존 기록과 새 기록을 함께 내보낸다.
                            historyFlow.value = listOf(earlierHistory, firstArg())
                            // 기록 흐름을 수집할 틈을 준 뒤 저장을 마친다.
                            delay(1L)
                        }

                        runScenario(
                            scenario = { testScope ->
                                containerHost.inputOnePlusTwo()
                                containerHost.processIntent(CalculatorContract.Intent.Calculate)
                                testScope.settle()
                            },
                            verify = { state, _ ->
                                state.histories shouldBe listOf(
                                    CalculatorContract.HistoryItem(expression = "7×6", result = "42"),
                                    CalculatorContract.HistoryItem(expression = "1+2", result = "3"),
                                )
                            },
                        )
                    }
                }
            }
        }
    })

/**
 * 지연을 모두 흘려 남은 작업을 끝낸다.
 *
 * orbit-test는 컨테이너를 `backgroundScope`에서 돌리는데, `advanceUntilIdle()`은 백그라운드 작업을 기다리지 않는다.
 * 그래서 시나리오의 가장 긴 지연보다 넉넉히 시간을 흘린 뒤 지금 실행할 수 있는 작업을 마저 돌린다.
 */
private fun TestScope.settle() {
    testScheduler.advanceTimeBy(1_000L)
    testScheduler.runCurrent()
}

/** 환율 조회 한 번의 응답. [error]가 있으면 [delayMillis] 뒤에 던진다. */
private data class RateStep(
    val delayMillis: Long = 0L,
    val rate: Double = 1.0,
    val error: Throwable? = null,
)

private fun CalculatorViewModel.inputOnePlusTwo() {
    processIntent(CalculatorContract.Intent.Input(CalculatorToken.Number("1")))
    processIntent(CalculatorContract.Intent.Input(CalculatorToken.Operator("+")))
    processIntent(CalculatorContract.Intent.Input(CalculatorToken.Number("2")))
}

/** 첫 USD 조회가 시작된 것을 확인한 뒤 EUR, USD를 고른다. 첫 조회가 진행 중일 때 다음 선택이 들어오게 하려는 것이다. */
private fun OrbitTestContext<
    CalculatorContract.State,
    CalculatorContract.SideEffect,
    CalculatorViewModel,
>.selectUsdEurUsd(
    usd: CurrencyInfo,
    eur: CurrencyInfo,
    testScope: TestScope,
) {
    containerHost.processIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(usd))
    testScope.testScheduler.runCurrent()
    containerHost.processIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(eur))
    containerHost.processIntent(CalculatorContract.Intent.SelectMainExchangeCurrency(usd))
    testScope.settle()
}

/**
 * 이미 받은 항목을 모두 꺼내 그중 SideEffect만 돌려준다.
 *
 * 작업을 모두 끝낸 뒤에 부르므로 남은 항목은 이미 쌓여 있다. 더 꺼낼 항목이 없으면 `awaitItem`이
 * [DRAIN_TIMEOUT]만큼 기다린 뒤 실패하는데, 그것을 끝 신호로 쓴다.
 * `expectNoItems`로 끝을 확인하지 않는 이유: 남은 항목이 있으면 그 항목을 꺼낸 채로 실패해 항목을 잃는다.
 */
private suspend fun OrbitTestContext<
    CalculatorContract.State,
    CalculatorContract.SideEffect,
    CalculatorViewModel,
>.drainSideEffects(): List<CalculatorContract.SideEffect> {
    val sideEffects = mutableListOf<CalculatorContract.SideEffect>()
    while (true) {
        val item = try {
            awaitItem()
        } catch (@Suppress("SwallowedException") noMoreItems: AssertionError) {
            return sideEffects
        }
        if (item is Item.SideEffectItem) sideEffects += item.value
    }
}

/** 남은 항목을 꺼낼 때 더 없다고 판단할 때까지의 실제 시간 */
private val DRAIN_TIMEOUT = 200.milliseconds
