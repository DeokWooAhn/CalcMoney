package com.ahn.presentation.main

import android.util.Log
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

    fun createViewModel() = MainViewModel(
        themeUseCases = ThemeUseCases(
            getThemeMode = getThemeModeUseCase,
            saveThemeMode = saveThemeModeUseCase,
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
    }
})
