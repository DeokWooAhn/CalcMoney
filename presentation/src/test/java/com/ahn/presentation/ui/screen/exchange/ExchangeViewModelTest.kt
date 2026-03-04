package com.ahn.presentation.ui.screen.exchange

import com.ahn.domain.model.CurrencyInfo
import com.ahn.domain.usecase.GetExchangeRateUseCase
import com.ahn.domain.usecase.GetSupportedCurrenciesUseCase
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.test

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeViewModelTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val testDispatcher = UnconfinedTestDispatcher()

    val mockCurrencies = listOf(
        CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸"),
        CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷"),
    )
    val usd = mockCurrencies[0]
    val krw = mockCurrencies[1]

    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()
    val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()

    beforeEach {
        Dispatchers.setMain(testDispatcher)
        coEvery { getSupportedCurrenciesUseCase() } returns mockCurrencies
        coEvery { getExchangeRateUseCase(any(), any()) } returns 1333.33
    }
    afterEach { Dispatchers.resetMain() }

    Given("ViewModel이 처음 생성되었을 때") {
        When("초기 상태를 확인하면") {
            Then("초기화 과정이 순차적으로 올바르게 진행되어야 한다") {
                runTest {
                    val viewModel =
                        ExchangeViewModel(getExchangeRateUseCase, getSupportedCurrenciesUseCase)

                    viewModel.test(this) {
                        expectInitialState()

                        runOnCreate() // 초기화 시작

                        // ─── [1단계] performLoadCurrencies() 구간 ───
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
                                exchangeRate = 1333.33,
                                toAmount = "1333.33" // 기본 금액 "1" * 1333.33
                            )
                        }
                    }
                }
            }
        }
    }

    Given("금액을 입력할 때") {
        When("유효한 금액을 입력하면") {
            Then("입력값과 환전 금액이 반영되어야 한다") {
                runTest {
                    val viewModel =
                        ExchangeViewModel(getExchangeRateUseCase, getSupportedCurrenciesUseCase)

                    viewModel.test(this) {
                        expectInitialState()

                        // 1. 초기화 시작
                        runOnCreate()

                        // ─── [초기화 상태 4개 모두 소비하기] ───
                        // (1) 통화 목록 로딩 켜짐
                        expectState { copy(isLoading = true) }

                        // (2) 통화 목록 세팅 완료, 로딩 꺼짐
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }

                        // (3) 자동으로 이어지는 환율 로딩 켜짐 (Actual 에러의 범인!)
                        expectState { copy(isLoading = true) }

                        // (4) 환율 세팅 완료, 로딩 꺼짐, 기본 계산(1 * 1333.33) 세팅
                        expectState {
                            copy(
                                isLoading = false,
                                exchangeRate = 1333.33,
                                toAmount = "1333.33"
                            )
                        }
                        // ────────────────────────────────

                        // 2. 이제 큐가 깨끗해졌으니 사용자 액션 실행
                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("1000"))

                        // 3. 액션에 대한 결과 검증
                        expectState {
                            copy(
                                fromAmount = "1000",
                                toAmount = "1333330.00"
                            )
                        }
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

                    val viewModel =
                        ExchangeViewModel(getExchangeRateUseCase, getSupportedCurrenciesUseCase)

                    viewModel.test(this) {
                        expectInitialState()

                        runOnCreate()

                        // 1. 로딩 켜짐
                        expectState { copy(isLoading = true) }

                        // 2. 에러가 발생하여 캐치(catch) 블록으로 이동, 로딩만 꺼지고 상태는 그대로
                        expectState { copy(isLoading = false) }

                        // 3. 에러 스낵바 SideEffect 발생 검증
                        expectSideEffect(
                            ExchangeContract.SideEffect.ShowSnackBar("통화 목록을 불러올 수 없습니다: Network Error")
                        )
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
                    coEvery {
                        getExchangeRateUseCase(any(), any())
                    } throws Exception("Network Error")

                    val viewModel =
                        ExchangeViewModel(getExchangeRateUseCase, getSupportedCurrenciesUseCase)

                    viewModel.test(this) {
                        expectInitialState()

                        runOnCreate()

                        // ─── [1단계: 통화 목록 로드 (성공)] ───
                        expectState { copy(isLoading = true) }
                        expectState {
                            copy(
                                isLoading = false,
                                availableCurrencies = mockCurrencies,
                                fromCurrency = usd,
                                toCurrency = krw
                            )
                        }

                        // ─── [2단계: 환율 로드 (여기서 실패!)] ───
                        expectState { copy(isLoading = true) }
                        expectState { copy(isLoading = false) } // 실패해서 로딩만 꺼짐

                        // SideEffect 검증
                        expectSideEffect(
                            ExchangeContract.SideEffect.ShowSnackBar("환율 정보를 가져올 수 없습니다: Network Error")
                        )
                    }
                }
            }
        }
    }
})
