package com.ahn.presentation.ui.screen.exchange

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
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.Container
import org.orbitmvi.orbit.ContainerDecorator
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.test.test

/**
 * 자동으로 돈 일의 실패는 side effect로 나가지 않고, 사용자가 누른 새로고침의 실패만 나간다는 것을 고정한다.
 * 그 위에서 Orbit의 side effect가 구독 공백 중 버퍼링되어 다음 구독자에게 발생 순서대로 전달되는지,
 * 동시 구독자가 둘이면 이벤트 하나를 합쳐 한 번만 받는 fan-out인지도 함께 본다. 수신자는 보장하지 않는다.
 * 기준은 `docs/decisions/event-scope-and-failure-state.md`.
 *
 * 첫 시나리오는 orbit-test가 LazyCreate를 우회한 라이브러리 수준 검사이며, 앱의 초기 로드 시점을 재현하지 않는다.
 * 구독 공백 시나리오는 탭 이동 모양만 흉내 내며 다음은 확인하지 않는다.
 * - 배달 도중 구독이 취소될 때의 유실: receiveAsFlow는 이때 항목을 잃을 수 있다.
 * - 기본 버퍼 한도 64 초과: postSideEffect가 suspend하여 해당 intent가 멈출 수 있다.
 * - 탭 전환 시 LaunchedEffect의 실제 취소 시점과 사용자 눈에 스낵바가 뜨는 화면.
 * - 백그라운드 전환으로 생기는 구독 공백과 복귀 시 동작.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeSideEffectScopeTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    lateinit var testDispatcher: TestDispatcher
    val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
    val refreshExchangeRatesUseCase = mockk<RefreshExchangeRatesUseCase>()
    val getFavoriteCurrenciesUseCase = mockk<GetFavoriteCurrenciesUseCase>()
    val loadFailure = ExchangeContract.SideEffect.ShowSnackBar(
        UiText.StringResource(R.string.exchange_rate_network_unavailable),
    )
    val refreshFailure = ExchangeContract.SideEffect.ShowSnackBar(
        UiText.StringResource(R.string.exchange_rate_temporarily_unavailable),
    )

    fun createViewModel() = ExchangeViewModel(
        exchangeUseCases = ExchangeUseCases(
            exchangeAmount = mockk<CalculateExchangeAmountUseCase>(),
            convertExchangeAmount = mockk<ConvertExchangeAmountUseCase>(),
            getExchangeRate = mockk<GetExchangeRateUseCase>(),
            getLatestRateDate = mockk<GetLatestExchangeRateDateUseCase>(),
            getLatestFetchedAt = mockk<GetLatestExchangeRateFetchedAtUseCase>(),
            refreshExchangeRates = refreshExchangeRatesUseCase,
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
            getExchangeSelection = mockk<GetExchangeSelectionUseCase>(),
            saveExchangeFromCurrency = mockk<SaveExchangeFromCurrencyUseCase>(),
            saveExchangeToCurrency = mockk<SaveExchangeToCurrencyUseCase>(),
            saveExchangeSelection = mockk<SaveExchangeSelectionUseCase>(),
        ),
    )

    suspend fun TestScope.awaitRequestFailure(
        viewModel: ExchangeViewModel,
        beforeResponse: suspend () -> Unit = {},
    ) {
        runCurrent()
        viewModel.container.stateFlow.value.isLoading shouldBe true
        beforeResponse()
        advanceTimeBy(RESPONSE_DELAY_MILLIS)
        runCurrent()
        viewModel.container.stateFlow.value.isLoading shouldBe false
    }

    beforeEach {
        clearAllMocks()
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        coEvery { getSupportedCurrenciesUseCase() } coAnswers {
            delay(RESPONSE_DELAY_MILLIS)
            throw ExchangeRateException.NetworkUnavailable()
        }
        coEvery { refreshExchangeRatesUseCase() } coAnswers {
            delay(RESPONSE_DELAY_MILLIS)
            throw ExchangeRateException.TemporarilyUnavailable()
        }
        every { getFavoriteCurrenciesUseCase() } returns MutableStateFlow(emptyList())
    }
    afterEach { Dispatchers.resetMain() }

    Given("orbit-test가 LazyCreate를 우회해 초기화를 직접 실행할 수 있는 컨테이너에서") {
        When("side effect 구독 없이 초기 로드와 새로고침을 실패시킨 뒤 구독하면") {
            Then("자동 로드 실패는 쌓이지 않고 사용자가 누른 새로고침 실패만 전달되어야 한다") {
                runTest(testDispatcher) {
                    val viewModel = createViewModel()
                    val received = mutableListOf<ExchangeContract.SideEffect>()

                    StateOnlyExchangeHost(viewModel).test(this) {
                        expectInitialState()
                        runOnCreate()
                        awaitRequestFailure(viewModel)

                        viewModel.processIntent(ExchangeContract.Intent.RefreshExchangeRates)
                        awaitRequestFailure(viewModel)
                        coVerify(exactly = 1) { getSupportedCurrenciesUseCase() }
                        coVerify(exactly = 1) { refreshExchangeRatesUseCase() }

                        // 실패 처리가 끝난 뒤에야 실제 side effect 구독을 시작한다.
                        val subscriber = backgroundScope.launch(testDispatcher) {
                            viewModel.container.refCountSideEffectFlow.collect { received += it }
                        }
                        try {
                            runCurrent()
                            received shouldBe listOf(refreshFailure)
                            received shouldNotContain loadFailure
                        } finally {
                            subscriber.cancelAndJoin()
                            cancelAndIgnoreRemainingItems()
                        }
                    }
                }
            }
        }
    }

    Given("첫 구독자가 붙어 있는 환율 ViewModel에서") {
        When("새로고침 응답 전에 구독을 끊고 구독 공백 중 두 번 더 실패한 뒤 다시 구독하면") {
            Then("새 구독자는 공백 중 발생한 두 실패만 순서대로 받아야 한다") {
                runTest(testDispatcher) {
                    val viewModel = createViewModel()
                    val firstReceived = mutableListOf<ExchangeContract.SideEffect>()
                    val nextReceived = mutableListOf<ExchangeContract.SideEffect>()

                    StateOnlyExchangeHost(viewModel).test(this) {
                        expectInitialState()
                        val firstSubscriber = backgroundScope.launch(testDispatcher) {
                            viewModel.container.refCountSideEffectFlow.collect { firstReceived += it }
                        }
                        runCurrent()
                        runOnCreate()
                        awaitRequestFailure(viewModel)
                        firstReceived shouldBe emptyList()

                        viewModel.processIntent(ExchangeContract.Intent.RefreshExchangeRates)
                        awaitRequestFailure(viewModel) {
                            // 응답을 기다리던 화면을 떠나 기존 구독이 완전히 끝난 뒤 실패하게 한다.
                            firstSubscriber.cancelAndJoin()
                            firstSubscriber.isCompleted shouldBe true
                        }

                        viewModel.processIntent(ExchangeContract.Intent.RefreshExchangeRates)
                        awaitRequestFailure(viewModel)
                        coVerify(exactly = 2) { refreshExchangeRatesUseCase() }
                        coVerify(exactly = 1) { getSupportedCurrenciesUseCase() }
                        firstReceived shouldBe emptyList()

                        val nextSubscriber = backgroundScope.launch(testDispatcher) {
                            viewModel.container.refCountSideEffectFlow.collect { nextReceived += it }
                        }
                        try {
                            runCurrent()
                            nextReceived shouldBe listOf(refreshFailure, refreshFailure)
                            firstReceived shouldBe emptyList()
                        } finally {
                            nextSubscriber.cancelAndJoin()
                            cancelAndIgnoreRemainingItems()
                        }
                    }
                }
            }
        }
    }

    Given("같은 환율 ViewModel의 side effect를 동시에 구독하는 구독자 둘이 있을 때") {
        When("사용자가 누른 새로고침이 실패해 이벤트 하나가 발생하면") {
            Then("어느 구독자가 받든 둘이 합쳐 실패 이벤트를 한 번만 받아야 한다") {
                runTest(testDispatcher) {
                    val viewModel = createViewModel()
                    val firstReceived = mutableListOf<ExchangeContract.SideEffect>()
                    val secondReceived = mutableListOf<ExchangeContract.SideEffect>()

                    StateOnlyExchangeHost(viewModel).test(this) {
                        expectInitialState()
                        val firstSubscriber = backgroundScope.launch(testDispatcher) {
                            viewModel.container.refCountSideEffectFlow.collect { firstReceived += it }
                        }
                        val secondSubscriber = backgroundScope.launch(testDispatcher) {
                            viewModel.container.refCountSideEffectFlow.collect { secondReceived += it }
                        }
                        try {
                            runCurrent()
                            firstSubscriber.isActive shouldBe true
                            secondSubscriber.isActive shouldBe true
                            firstReceived shouldBe emptyList()
                            secondReceived shouldBe emptyList()

                            runOnCreate()
                            awaitRequestFailure(viewModel)
                            firstReceived shouldBe emptyList()
                            secondReceived shouldBe emptyList()

                            viewModel.processIntent(ExchangeContract.Intent.RefreshExchangeRates)
                            awaitRequestFailure(viewModel)

                            coVerify(exactly = 1) { refreshExchangeRatesUseCase() }
                            (firstReceived + secondReceived) shouldBe listOf(refreshFailure)
                        } finally {
                            firstSubscriber.cancelAndJoin()
                            secondSubscriber.cancelAndJoin()
                            cancelAndIgnoreRemainingItems()
                        }
                    }
                }
            }
        }
    }
})

/**
 * orbit-test의 자동 side effect 구독만 막고, 상태 관찰·초기화·테스트 컨테이너 설정은 그대로 사용한다.
 * 자동 구독이 이벤트를 먼저 가져가면 구독자가 없는 상황을 검사할 수 없다.
 * 실제 이벤트는 각 시나리오가 화면과 같은 원래 ViewModel의 refCountSideEffectFlow에서 직접 수집한다.
 */
private class StateOnlyExchangeHost(viewModel: ExchangeViewModel) :
    ContainerHost<ExchangeContract.State, ExchangeContract.SideEffect> {
    override val container = object : ContainerDecorator<ExchangeContract.State, ExchangeContract.SideEffect> {
        override val actual: Container<ExchangeContract.State, ExchangeContract.SideEffect> = viewModel.container
        override val sideEffectFlow = emptyFlow<ExchangeContract.SideEffect>()
    }
}

private const val RESPONSE_DELAY_MILLIS = 100L
