package com.ahn.presentation.main

import android.util.Log
import com.ahn.domain.ads.usecase.InterstitialAdUseCases
import com.ahn.domain.ads.usecase.RecordInterstitialAdCalculationUseCase
import com.ahn.domain.ads.usecase.RecordInterstitialAdShownUseCase
import com.ahn.domain.ads.usecase.ShouldShowInterstitialAdUseCase
import com.ahn.domain.review.usecase.RecordAppOpenUseCase
import com.ahn.domain.review.usecase.RecordCalculationSuccessUseCase
import com.ahn.domain.review.usecase.ReviewPromptUseCases
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.domain.setting.usecase.GetThemeModeUseCase
import com.ahn.domain.setting.usecase.SaveThemeModeUseCase
import com.ahn.domain.setting.usecase.ThemeUseCases
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val testDispatcher = UnconfinedTestDispatcher()
    val getThemeModeUseCase = mockk<GetThemeModeUseCase>()
    val saveThemeModeUseCase = mockk<SaveThemeModeUseCase>()
    val recordAppOpenUseCase = mockk<RecordAppOpenUseCase>()
    val recordCalculationSuccessUseCase = mockk<RecordCalculationSuccessUseCase>()
    val recordInterstitialAdCalculationUseCase = mockk<RecordInterstitialAdCalculationUseCase>()
    val shouldShowInterstitialAdUseCase = mockk<ShouldShowInterstitialAdUseCase>()
    val recordInterstitialAdShownUseCase = mockk<RecordInterstitialAdShownUseCase>()

    fun createViewModel() = MainViewModel(
        themeUseCases = ThemeUseCases(
            getThemeMode = getThemeModeUseCase,
            saveThemeMode = saveThemeModeUseCase,
        ),
        reviewPromptUseCases = ReviewPromptUseCases(
            recordAppOpen = recordAppOpenUseCase,
            recordCalculationSuccess = recordCalculationSuccessUseCase,
        ),
        interstitialAdUseCases = InterstitialAdUseCases(
            recordCalculation = recordInterstitialAdCalculationUseCase,
            shouldShow = shouldShowInterstitialAdUseCase,
            recordShown = recordInterstitialAdShownUseCase,
        ),
    )

    beforeSpec {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
    }

    afterSpec {
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    beforeEach {
        clearAllMocks()
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { getThemeModeUseCase() } returns emptyFlow()
        coEvery { saveThemeModeUseCase(any()) } returns Unit
        coEvery { recordAppOpenUseCase() } returns Unit
        coEvery { shouldShowInterstitialAdUseCase() } returns true
        coEvery { recordInterstitialAdShownUseCase() } returns Unit
    }

    Given("메인 ViewModel에서") {
        When("테마 Flow가 아직 값을 방출하지 않으면") {
            Then("초기 테마 모드는 시스템 모드여야 한다") {
                val viewModel = createViewModel()

                viewModel.themeMode.value shouldBe ThemeMode.SYSTEM
            }
        }

        When("저장된 테마 모드가 변경되면") {
            Then("themeMode 상태에 반영되어야 한다") {
                runTest {
                    val themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
                    every { getThemeModeUseCase() } returns themeModeFlow

                    val viewModel = createViewModel()
                    viewModel.themeMode.launchIn(backgroundScope)

                    themeModeFlow.value = ThemeMode.DARK
                    advanceUntilIdle()

                    viewModel.themeMode.value shouldBe ThemeMode.DARK
                }
            }
        }

        When("테마 모드 저장을 요청하면") {
            Then("저장 UseCase를 호출해야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.saveThemeMode(ThemeMode.LIGHT)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { saveThemeModeUseCase(ThemeMode.LIGHT) }
                }
            }
        }

        When("테마 모드 저장 중 일반 예외가 발생하면") {
            Then("예외를 전파하지 않고 로그로 처리해야 한다") {
                runTest {
                    coEvery { saveThemeModeUseCase(ThemeMode.DARK) } throws IllegalStateException("save failed")
                    val viewModel = createViewModel()

                    viewModel.saveThemeMode(ThemeMode.DARK)
                    advanceUntilIdle()

                    coVerify(exactly = 1) { saveThemeModeUseCase(ThemeMode.DARK) }
                    verify(exactly = 1) {
                        Log.w("MainViewModel", "Failed to save theme mode.", any<Throwable>())
                    }
                }
            }
        }

        When("ViewModel이 만들어지기만 하면") {
            Then("앱 열기를 기록하지 않아야 한다") {
                createViewModel()

                coVerify(exactly = 0) { recordAppOpenUseCase() }
            }
        }

        When("앱이 앞으로 올 때마다 앱 열기 기록을 요청하면") {
            Then("요청한 횟수만큼 기록 UseCase를 호출해야 한다") {
                val viewModel = createViewModel()

                viewModel.recordAppOpen()
                viewModel.recordAppOpen()

                coVerify(exactly = 2) { recordAppOpenUseCase() }
            }
        }

        When("앱 열기 기록 중 일반 예외가 발생하면") {
            Then("예외를 전파하지 않고 로그로 처리해야 한다") {
                coEvery { recordAppOpenUseCase() } throws IllegalStateException("record failed")

                createViewModel().recordAppOpen()

                verify(exactly = 1) {
                    Log.w("MainViewModel", "Failed to record app open.", any<Throwable>())
                }
            }
        }
    }

    Given("계산기를 떠날 때 전면 광고를 확인하면") {
        When("광고 조건을 채웠으면") {
            Then("광고를 보여 줄 차례라고 알려야 한다") {
                runTest {
                    createViewModel().shouldShowInterstitialAd() shouldBe true
                }
            }
        }

        When("이번 실행에서 인앱 리뷰를 요청했으면") {
            Then("광고 조건과 관계없이 보여 주지 않아야 한다") {
                runTest {
                    val viewModel = createViewModel()

                    viewModel.markReviewRequested()

                    viewModel.shouldShowInterstitialAd() shouldBe false
                    coVerify(exactly = 0) { shouldShowInterstitialAdUseCase() }
                }
            }
        }

        When("광고 기록을 읽는 사이 인앱 리뷰를 요청하면") {
            Then("기록을 다 읽은 뒤 다시 확인해 보여 주지 않아야 한다") {
                runTest {
                    val viewModel = createViewModel()
                    coEvery { shouldShowInterstitialAdUseCase() } coAnswers {
                        viewModel.markReviewRequested()
                        true
                    }

                    viewModel.shouldShowInterstitialAd() shouldBe false
                }
            }
        }

        When("광고 기록을 읽다가 일반 예외가 발생하면") {
            Then("광고를 건너뛰고 로그로 남겨야 한다") {
                runTest {
                    coEvery { shouldShowInterstitialAdUseCase() } throws IllegalStateException("read failed")

                    createViewModel().shouldShowInterstitialAd() shouldBe false

                    verify(exactly = 1) {
                        Log.w("MainViewModel", "Failed to check interstitial ad record.", any<Throwable>())
                    }
                }
            }
        }

        When("광고를 보여 줬다고 기록하면") {
            Then("기록 UseCase를 호출해야 한다") {
                createViewModel().recordInterstitialAdShown()

                coVerify(exactly = 1) { recordInterstitialAdShownUseCase() }
            }
        }

        When("광고를 보여 줬다는 기록 중 일반 예외가 발생하면") {
            Then("예외를 전파하지 않고 로그로 처리해야 한다") {
                coEvery { recordInterstitialAdShownUseCase() } throws IllegalStateException("save failed")

                createViewModel().recordInterstitialAdShown()

                verify(exactly = 1) {
                    Log.w("MainViewModel", "Failed to record interstitial ad shown.", any<Throwable>())
                }
            }
        }
    }
})
