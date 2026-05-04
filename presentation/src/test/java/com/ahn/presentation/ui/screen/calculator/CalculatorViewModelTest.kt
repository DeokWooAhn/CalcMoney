package com.ahn.presentation.ui.screen.calculator

import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.usecase.CalculatorEngine
import com.ahn.domain.usecase.GetExchangeRateUseCase
import com.ahn.domain.usecase.GetSupportedCurrenciesUseCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerRoot

    val testDispatcher = UnconfinedTestDispatcher()
    val calculatorEngine = mockk<CalculatorEngine>()
    val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()

    fun createViewModel() = CalculatorViewModel(
        calculatorEngine = calculatorEngine,
        getSupportedCurrenciesUseCase = getSupportedCurrenciesUseCase,
        getExchangeRateUseCase = getExchangeRateUseCase,
    )

    beforeSpec { Dispatchers.setMain(testDispatcher) }
    afterSpec { Dispatchers.resetMain() }

    // InstancePerRoot에서는 매 Then마다 Mock 객체를 초기화해야 함
    beforeEach {
        clearAllMocks()
        every { calculatorEngine.calculate(any()) } returns "0"
    }

    Given("계산기 초기 상태에서") {
        When("5를 입력하면") {
            Then("expression은 '5'이어야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number(
                                    "5"
                                )
                            )
                        )
                        expectState { copy(expression = "5", cursorPosition = 1) }
                    }
                }
            }
        }

        When("0을 입력하면") {
            Then("expression은 '0'이어야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()
                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number(
                                    "0"
                                )
                            )
                        )
                        expectState { copy(expression = "0", cursorPosition = 1) }
                    }
                }
            }
        }

        When("연산자를 먼저 입력하면") {
            Then("무시되어 expression은 비어 있어야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(CalculatorToken.Operator("+"))
                        )
                    }
                }
            }
        }
    }

    Given("보조 환율 통화가 선택된 상태에서") {
        When("숫자를 입력하면") {
            Then("선택된 환율로 변환 금액이 표시되고 API는 다시 호출되지 않아야 한다") {
                runTest {
                    val vnd = CurrencyInfo(
                        code = "VND",
                        displayCode = "VND",
                        name = "베트남 동",
                        flagEmoji = "🇻🇳",
                    )
                    val krw = CurrencyInfo(
                        code = "KRW",
                        displayCode = "KRW",
                        name = "대한민국 원",
                        flagEmoji = "🇰🇷",
                    )

                    coEvery { getExchangeRateUseCase("KRW", "VND") } returns 18.5

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        containerHost.processIntent(
                            CalculatorContract.Intent.SelectMainExchangeCurrency(krw)
                        )
                        expectState { copy(mainExchangeCurrency = krw) }

                        containerHost.processIntent(
                            CalculatorContract.Intent.SelectExchangeCurrency(vnd)
                        )
                        expectState { copy(selectedExchangeCurrency = vnd) }
                        expectState { copy(exchangeRate = 18.5) }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(CalculatorToken.Number("2"))
                        )
                        expectState {
                            copy(
                                expression = "2",
                                cursorPosition = 1,
                                convertedExpressionAmount = "37 VND",
                            )
                        }

                        coVerify(exactly = 1) { getExchangeRateUseCase("KRW", "VND") }
                    }
                }
            }
        }
    }

    Given("메인 환율과 보조 환율이 선택된 상태에서") {
        When("스왑 버튼을 누르면") {
            Then("두 통화가 서로 바뀌고 바뀐 방향의 환율을 다시 가져와야 한다") {
                runTest {
                    val krw = CurrencyInfo(
                        code = "KRW",
                        displayCode = "KRW",
                        name = "대한민국 원",
                        flagEmoji = "🇰🇷",
                    )
                    val usd = CurrencyInfo(
                        code = "USD",
                        displayCode = "USD",
                        name = "미국 달러",
                        flagEmoji = "🇺🇸",
                    )

                    coEvery { getExchangeRateUseCase("KRW", "USD") } returns 0.001
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1000.0

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        containerHost.processIntent(
                            CalculatorContract.Intent.SelectMainExchangeCurrency(krw)
                        )
                        expectState { copy(mainExchangeCurrency = krw) }

                        containerHost.processIntent(
                            CalculatorContract.Intent.SelectExchangeCurrency(usd)
                        )
                        expectState { copy(selectedExchangeCurrency = usd) }
                        expectState { copy(exchangeRate = 0.001) }

                        containerHost.processIntent(CalculatorContract.Intent.SwapExchangeCurrencies)
                        expectState {
                            copy(
                                mainExchangeCurrency = usd,
                                selectedExchangeCurrency = krw,
                                exchangeRate = 0.0,
                            )
                        }
                        expectState { copy(exchangeRate = 1000.0) }

                        coVerify(exactly = 1) { getExchangeRateUseCase("KRW", "USD") }
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "KRW") }
                    }
                }
            }
        }
    }

    Given("'123'이 입력된 상태에서") {
        When("AC를 누르면") {
            Then("expression은 비어있고 커서 위치는 0이어야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        // 1. "123" 셋업 (명시적 상태 추적)
                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number(
                                    "1"
                                )
                            )
                        )
                        expectState { copy(expression = "1", cursorPosition = 1) }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number(
                                    "2"
                                )
                            )
                        )
                        expectState { copy(expression = "12", cursorPosition = 2) }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number(
                                    "3"
                                )
                            )
                        )
                        expectState { copy(expression = "123", cursorPosition = 3) }

                        // 2. AC 액션
                        containerHost.processIntent(CalculatorContract.Intent.Clear)

                        // 3. 결과 검증 (초기 상태로 완벽히 복귀했는지 확인)
                        expectState { CalculatorContract.State() }
                    }
                }
            }

            When("삭제(⌫)를 누르면") {
                Then("마지막 글자가 지워져 '12'가 되어야 한다") {
                    runTest {
                        val viewModel = createViewModel()

                        viewModel.test(this) {
                            expectInitialState()

                            // 1. "123" 셋업
                            containerHost.processIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("1")
                                )
                            )
                            expectState { copy(expression = "1", cursorPosition = 1) }

                            containerHost.processIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("2")
                                )
                            )
                            expectState { copy(expression = "12", cursorPosition = 2) }

                            containerHost.processIntent(
                                CalculatorContract.Intent.Input(
                                    CalculatorToken.Number("3")
                                )
                            )
                            expectState { copy(expression = "123", cursorPosition = 3) }

                            // 2. 삭제 액션
                            containerHost.processIntent(CalculatorContract.Intent.Delete)

                            // 3. 결과 검증
                            expectState { copy(expression = "12", cursorPosition = 2) }
                        }
                    }
                }
            }
        }
    }

    Given("'10+20'이 입력된 상태에서") {
        When("=을 누르면") {
            Then("결과 '30'이 expression에 표시되어야 한다") {
                runTest {
                    // InstancePerRoot에서는 Mocking을 Then 블록 안에 두는 것이 가장 안전
                    every { calculatorEngine.calculate(any()) } returns "0"
                    every { calculatorEngine.calculate("10+2") } returns "12"
                    every { calculatorEngine.calculate("10+20") } returns "30"

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        // 1. "10+20" 셋업
                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number("1")
                            )
                        )
                        expectState { copy(expression = "1", cursorPosition = 1, previewResult = "") }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number("0")
                            )
                        )
                        expectState { copy(expression = "10", cursorPosition = 2, previewResult = "") }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Operator("+")
                            )
                        )
                        expectState { copy(expression = "10+", cursorPosition = 3, previewResult = "") }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number("2")
                            )
                        )
                        expectState { copy(expression = "10+2", cursorPosition = 4, previewResult = "12") }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number("0")
                            )
                        )
                        expectState { copy(expression = "10+20", cursorPosition = 5, previewResult = "30") }

                        // 2. 계산 액션
                        containerHost.processIntent(CalculatorContract.Intent.Calculate)

                        // 3. 결과 검증 ("30"의 문자열 길이는 2이므로 커서도 2)
                        expectState {
                            copy(
                                expression = "30",
                                cursorPosition = 2,
                                previewResult = "",
                                repeatOperation = "+20",
                            )
                        }
                    }
                }
            }
        }
    }

    Given("'2+1'을 계산한 상태에서") {
        When("=을 반복해서 누르면") {
            Then("마지막 연산 '+1'이 계속 반복되어야 한다") {
                runTest {
                    every { calculatorEngine.calculate(any()) } returns "0"
                    every { calculatorEngine.calculate("2+1") } returns "3"
                    every { calculatorEngine.calculate("3+1") } returns "4"
                    every { calculatorEngine.calculate("4+1") } returns "5"

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(CalculatorToken.Number("2"))
                        )
                        expectState {
                            copy(
                                expression = "2",
                                cursorPosition = 1,
                                previewResult = "",
                            )
                        }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(CalculatorToken.Operator("+"))
                        )
                        expectState {
                            copy(
                                expression = "2+",
                                cursorPosition = 2,
                                previewResult = "",
                            )
                        }

                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(CalculatorToken.Number("1"))
                        )
                        expectState {
                            copy(
                                expression = "2+1",
                                cursorPosition = 3,
                                previewResult = "3",
                            )
                        }

                        containerHost.processIntent(CalculatorContract.Intent.Calculate)
                        expectState {
                            copy(
                                expression = "3",
                                cursorPosition = 1,
                                previewResult = "",
                                repeatOperation = "+1",
                            )
                        }

                        containerHost.processIntent(CalculatorContract.Intent.Calculate)
                        expectState {
                            copy(
                                expression = "4",
                                cursorPosition = 1,
                                previewResult = "",
                                repeatOperation = "+1",
                            )
                        }

                        containerHost.processIntent(CalculatorContract.Intent.Calculate)
                        expectState {
                            copy(
                                expression = "5",
                                cursorPosition = 1,
                                previewResult = "",
                                repeatOperation = "+1",
                            )
                        }
                    }
                }
            }
        }
    }

    Given("계산 결과가 에러일 때") {
        When("=을 누르면") {
            Then("에러 상태가 되고 스낵바 SideEffect가 발생해야 한다") {
                runTest {
                    every { calculatorEngine.calculate(any()) } returns "Error"

                    val viewModel = createViewModel()

                    viewModel.test(this) {
                        expectInitialState()

                        // 1. 수식 입력
                        containerHost.processIntent(
                            CalculatorContract.Intent.Input(
                                CalculatorToken.Number("1")
                            )
                        )
                        expectState { copy(expression = "1", cursorPosition = 1) }

                        // 2. 계산 액션 발생
                        containerHost.processIntent(CalculatorContract.Intent.Calculate)

                        // 3. 상태 검증 (에러 상태로 변경됨)
                        expectState {
                            copy(isError = true, errorMessage = "계산 오류")
                        }

                        // 4. Turbine 없이 Orbit 내장 기능으로 SideEffect 검증
                        expectSideEffect(
                            CalculatorContract.SideEffect.ShowSnackBar("계산할 수 없는 수식입니다.")
                        )
                    }
                }
            }
        }
    }
})
