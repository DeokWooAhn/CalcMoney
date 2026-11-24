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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.orbitmvi.orbit.test.Item
import org.orbitmvi.orbit.test.OrbitTestContext
import org.orbitmvi.orbit.test.test
import java.util.Locale

private typealias ExchangeTestContext =
    OrbitTestContext<ExchangeContract.State, ExchangeContract.SideEffect, ExchangeViewModel>

/**
 * 상태 방출을 차례로 꺼내 즐겨찾기 카드 상태가 조건을 만족할 때까지 기다린다. 끝내 오지 않으면 시간 초과로 실패한다.
 * [seen]을 넘기면 지나간 카드 상태를 모두 담는다(중간에 틀린 카드가 잠깐이라도 나왔는지 볼 때).
 */
private suspend fun ExchangeTestContext.awaitFavoriteRates(
    seen: MutableList<ExchangeContract.FavoriteRates>? = null,
    predicate: (ExchangeContract.FavoriteRates) -> Boolean,
): ExchangeContract.FavoriteRates {
    while (true) {
        val item = awaitItem()
        if (item !is Item.StateItem) continue
        seen?.add(item.value.favoriteRates)
        if (predicate(item.value.favoriteRates)) {
            return item.value.favoriteRates
        }
    }
}

private fun ExchangeTestContext.currentFavoriteRates(): ExchangeContract.FavoriteRates =
    containerHost.container.stateFlow.value.favoriteRates

private fun ExchangeContract.FavoriteRates.codes() = items.map { it.currency.code }

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangeFavoriteRatesTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val testDispatcher = UnconfinedTestDispatcher()

    // 기기 지역을 한국으로 고정하면 처음 통화는 미국 달러 → 대한민국 원이다.
    // 환율 화면 자체의 조회(USD→KRW)와 겹치지 않게 즐겨찾기는 JPY·EUR로 둔다.
    val usd = CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸")
    val krw = CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷")
    val jpy = CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵")
    val eur = CurrencyInfo("EUR", "EUR", "유로", "🇪🇺")
    val currencies = listOf(usd, krw, jpy, eur)

    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()
    val getLatestExchangeRateDateUseCase = mockk<GetLatestExchangeRateDateUseCase>()
    val getLatestExchangeRateFetchedAtUseCase = mockk<GetLatestExchangeRateFetchedAtUseCase>()
    val getSupportedCurrenciesUseCase = mockk<GetSupportedCurrenciesUseCase>()
    val refreshExchangeRatesUseCase = mockk<RefreshExchangeRatesUseCase>()
    val getFavoriteCurrenciesUseCase = mockk<GetFavoriteCurrenciesUseCase>()
    val toggleFavoriteCurrencyUseCase = mockk<ToggleFavoriteCurrencyUseCase>()
    val calculateExchangeAmountUseCase = mockk<CalculateExchangeAmountUseCase>()
    val getExchangeSelectionUseCase = mockk<GetExchangeSelectionUseCase>()
    val saveExchangeFromCurrencyUseCase = mockk<SaveExchangeFromCurrencyUseCase>()
    val saveExchangeToCurrencyUseCase = mockk<SaveExchangeToCurrencyUseCase>()
    val saveExchangeSelectionUseCase = mockk<SaveExchangeSelectionUseCase>()

    lateinit var favoritesFlow: MutableStateFlow<List<String>>

    fun createViewModel() = ExchangeViewModel(
        exchangeUseCases = ExchangeUseCases(
            exchangeAmount = calculateExchangeAmountUseCase,
            convertExchangeAmount = mockk<ConvertExchangeAmountUseCase>(),
            getExchangeRate = getExchangeRateUseCase,
            getLatestRateDate = getLatestExchangeRateDateUseCase,
            getLatestFetchedAt = getLatestExchangeRateFetchedAtUseCase,
            refreshExchangeRates = refreshExchangeRatesUseCase,
            getSupportedCurrencies = getSupportedCurrenciesUseCase,
        ),
        favoriteUseCases = FavoriteUseCases(
            buildFavoriteRates = BuildFavoriteRatesUseCase(),
            getFavoriteCurrencies = getFavoriteCurrenciesUseCase,
            toggleFavoriteCurrency = toggleFavoriteCurrencyUseCase,
        ),
        currencySelectionUseCases = CurrencySelectionUseCases(
            getCalculatorSelection = mockk<GetCalculatorSelectionUseCase>(),
            saveCalculatorMainCurrency = mockk<SaveCalculatorMainCurrencyUseCase>(),
            saveCalculatorSubCurrency = mockk<SaveCalculatorSubCurrencyUseCase>(),
            saveCalculatorSelection = mockk<SaveCalculatorSelectionUseCase>(),
            getExchangeSelection = getExchangeSelectionUseCase,
            saveExchangeFromCurrency = saveExchangeFromCurrencyUseCase,
            saveExchangeToCurrency = saveExchangeToCurrencyUseCase,
            saveExchangeSelection = saveExchangeSelectionUseCase,
        ),
    )

    val originalLocale = Locale.getDefault()

    beforeEach {
        clearAllMocks()
        Locale.setDefault(Locale.KOREA)
        Dispatchers.setMain(testDispatcher)
        favoritesFlow = MutableStateFlow(emptyList())
        coEvery { getSupportedCurrenciesUseCase() } returns currencies
        coEvery { getExchangeRateUseCase(any(), any()) } returns 1500.0
        coEvery { getLatestExchangeRateDateUseCase() } returns "20261124"
        coEvery { getLatestExchangeRateFetchedAtUseCase() } returns 1000L
        coEvery { refreshExchangeRatesUseCase() } returns Unit
        every { getFavoriteCurrenciesUseCase() } returns favoritesFlow
        every { calculateExchangeAmountUseCase(any(), any()) } returns "1500.00"
        coEvery { getExchangeSelectionUseCase() } returns ExchangeCurrencySelection(fromCode = null, toCode = null)
        coEvery { saveExchangeFromCurrencyUseCase(any()) } returns Unit
        coEvery { saveExchangeToCurrencyUseCase(any()) } returns Unit
        coEvery { saveExchangeSelectionUseCase(any(), any()) } returns Unit
    }
    afterEach {
        Dispatchers.resetMain()
        Locale.setDefault(originalLocale)
    }

    /** onCreate를 돌리고 즐겨찾기 카드가 다 만들어질 때까지 기다린다. */
    suspend fun ExchangeTestContext.startAndAwaitCards(vararg codes: String): ExchangeContract.FavoriteRates {
        expectInitialState()
        runOnCreate()
        return awaitFavoriteRates { !it.isLoading && it.codes() == codes.toList() }
    }

    fun TestScope.runCurrentScheduler() = testScheduler.runCurrent()

    Given("환율 ViewModel이 즐겨찾기 카드를 만들 때") {
        When("즐겨찾기 환율을 조회하는 동안") {
            Then("로딩을 켰다가 조회가 끝나면 카드와 함께 끈다") {
                runTest {
                    val usdJpy = CompletableDeferred<Double>()
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } coAnswers { usdJpy.await() }

                    createViewModel().test(this) {
                        expectInitialState()
                        runOnCreate()

                        awaitFavoriteRates { it.isLoading } shouldBe ExchangeContract.FavoriteRates(
                            isLoading = true,
                            items = emptyList(),
                            baseOnlyFavoriteCode = null,
                        )

                        usdJpy.complete(9.5)

                        val loaded = awaitFavoriteRates { !it.isLoading }
                        loaded.codes() shouldContainExactly listOf("JPY")
                        loaded.items.single().convertedAmount shouldBe "9.50"
                        loaded.items.single().rateLabel shouldBe "1 USD = 9.5000 JPY"
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("기준 금액만 바뀌면") {
            Then("환율을 다시 조회하지 않고 모은 환율로 카드 금액만 다시 계산한다") {
                runTest {
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5

                    createViewModel().test(this) {
                        startAndAwaitCards("JPY")

                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("2.5"))

                        val updated = awaitFavoriteRates { it.items.singleOrNull()?.convertedAmount == "23.75" }
                        updated.isLoading shouldBe false
                        updated.items.single().rateLabel shouldBe "1 USD = 9.5000 JPY"
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("같은 즐겨찾기 입력으로 상태가 다시 방출되면(도착 통화 변경으로 로딩이 오가도)") {
            Then("이미 만든 카드를 유지하고 환율을 다시 조회하지 않는다") {
                runTest {
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5
                    coEvery { getExchangeRateUseCase("USD", "EUR") } returns 0.86

                    createViewModel().test(this) {
                        val loaded = startAndAwaitCards("JPY")

                        // 도착 통화만 바꾼다. 환율 화면의 isLoading·exchangeRate가 바뀌어 상태는 여러 번 방출되지만
                        // 즐겨찾기 입력(기준 통화, 즐겨찾기, 통화 목록, 환율 날짜·받은 시각)은 그대로다.
                        containerHost.processIntent(ExchangeContract.Intent.SelectToCurrency(eur))
                        val afterToChange = awaitItemsUntil { it.exchangeRate == 0.86 && !it.isLoading }
                        runCurrentScheduler()

                        afterToChange.favoriteRates shouldBe loaded
                        currentFavoriteRates() shouldBe loaded
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "EUR") }
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("기준 통화만 즐겨찾기된 상태이면") {
            Then("안내 상태를 만들고 환율을 조회하지 않으며, 금액이 바뀌어도 그대로다") {
                runTest {
                    favoritesFlow.value = listOf("USD")

                    createViewModel().test(this) {
                        expectInitialState()
                        runOnCreate()

                        awaitFavoriteRates { it.baseOnlyFavoriteCode != null } shouldBe ExchangeContract.FavoriteRates(
                            isLoading = false,
                            items = emptyList(),
                            baseOnlyFavoriteCode = "USD",
                        )

                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("10"))
                        awaitItemsUntil { it.fromAmount == "10" }
                        runCurrentScheduler()

                        currentFavoriteRates() shouldBe ExchangeContract.FavoriteRates(
                            isLoading = false,
                            items = emptyList(),
                            baseOnlyFavoriteCode = "USD",
                        )
                        coVerify(exactly = 0) { getExchangeRateUseCase("USD", "USD") }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("기준 통화만 즐겨찾기된 상태에서 기준 통화를 바꾸면") {
            Then("기존 기준 통화의 카드를 보여 주고 안내 상태를 해제한다") {
                runTest {
                    favoritesFlow.value = listOf("USD")
                    coEvery { getExchangeRateUseCase("KRW", "USD") } returns 0.00067

                    createViewModel().test(this) {
                        expectInitialState()
                        runOnCreate()
                        awaitFavoriteRates { it.baseOnlyFavoriteCode == "USD" }

                        // 도착 통화(KRW)를 기준으로 고르면 서로 바뀌어 KRW → USD가 된다.
                        containerHost.processIntent(ExchangeContract.Intent.SelectFromCurrency(krw))

                        val switched = awaitFavoriteRates { !it.isLoading && it.items.isNotEmpty() }
                        switched.baseOnlyFavoriteCode shouldBe null
                        switched.codes() shouldContainExactly listOf("USD")
                        switched.items.single().rateLabel shouldBe "1 KRW = 0.0007 USD"
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("즐겨찾기 조건이 사라지면") {
            Then("이전 카드를 비우고, 금액이 바뀌어도 이전 환율로 카드를 되살리지 않는다") {
                runTest {
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5

                    createViewModel().test(this) {
                        startAndAwaitCards("JPY")

                        favoritesFlow.value = emptyList()
                        awaitFavoriteRates { it.items.isEmpty() } shouldBe ExchangeContract.FavoriteRates()

                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("10"))
                        awaitItemsUntil { it.fromAmount == "10" }
                        runCurrentScheduler()

                        currentFavoriteRates() shouldBe ExchangeContract.FavoriteRates()
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("기준 통화를 빠르게 두 번 바꾸면") {
            Then("앞 조회가 늦게 끝나도 뒤 기준 통화의 카드를 덮지 않는다") {
                runTest {
                    val usdJpy = CompletableDeferred<Double>()
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } coAnswers { usdJpy.await() }
                    coEvery { getExchangeRateUseCase("EUR", "JPY") } returns 160.0

                    createViewModel().test(this) {
                        expectInitialState()
                        runOnCreate()
                        // USD 기준 조회가 끝나지 않은 채 멈춰 있다.
                        awaitFavoriteRates { it.isLoading }

                        containerHost.processIntent(ExchangeContract.Intent.SelectFromCurrency(eur))
                        val eurCards = awaitFavoriteRates { !it.isLoading && it.items.isNotEmpty() }
                        eurCards.items.single().rateLabel shouldBe "1 EUR = 160.0000 JPY"

                        // 앞(USD) 조회를 이제야 끝낸다. 이미 취소됐으므로 결과가 반영되면 안 된다.
                        usdJpy.complete(9.5)
                        runCurrentScheduler()

                        currentFavoriteRates() shouldBe eurCards
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }
                        coVerify(exactly = 1) { getExchangeRateUseCase("EUR", "JPY") }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("새로고침이 성공해 환율을 받은 시각이 바뀌면") {
            Then("즐겨찾기 환율을 다시 조회해 카드를 바꾼다") {
                runTest {
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returnsMany listOf(9.5, 9.8)

                    createViewModel().test(this) {
                        startAndAwaitCards("JPY")
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }

                        coEvery { getLatestExchangeRateFetchedAtUseCase() } returns 2000L
                        containerHost.processIntent(ExchangeContract.Intent.RefreshExchangeRates)

                        val refreshed = awaitFavoriteRates { it.items.singleOrNull()?.convertedAmount == "9.80" }
                        refreshed.isLoading shouldBe false
                        refreshed.items.single().rateLabel shouldBe "1 USD = 9.8000 JPY"
                        coVerify(exactly = 2) { getExchangeRateUseCase("USD", "JPY") }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("즐겨찾기 조회가 실패한 뒤 같은 서버 문서로 새로고침이 성공하면") {
            Then("환율 날짜·받은 시각이 그대로여도 즐겨찾기 환율을 다시 조회해 카드를 만든다") {
                runTest {
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } throws Exception("JPY failed") andThen 9.5

                    createViewModel().test(this) {
                        expectInitialState()
                        runOnCreate()
                        // 조회 실패 뒤의 카드 상태는 기본값과 같으므로 로딩이 켜졌다 꺼지는 순서로 기다린다.
                        awaitFavoriteRates { it.isLoading }
                        val failed = awaitFavoriteRates { !it.isLoading }
                        failed shouldBe ExchangeContract.FavoriteRates()
                        coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }

                        // 날짜·받은 시각을 바꾸지 않는다(beforeEach의 값 그대로).
                        containerHost.processIntent(ExchangeContract.Intent.RefreshExchangeRates)

                        val refreshed = awaitFavoriteRates { it.codes() == listOf("JPY") && !it.isLoading }
                        refreshed.items.single().convertedAmount shouldBe "9.50"
                        coVerify(exactly = 2) { getExchangeRateUseCase("USD", "JPY") }
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("즐겨찾기를 더해 재조회가 끝나지 않은 동안 금액을 바꾸면") {
            Then("조회가 끝나기 전에 기존 카드 금액을 새 금액으로 바꾸고, 조회가 끝나면 새 즐겨찾기까지 새 금액으로 보여 준다") {
                runTest {
                    val usdEur = CompletableDeferred<Double>()
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5
                    coEvery { getExchangeRateUseCase("USD", "EUR") } coAnswers { usdEur.await() }

                    createViewModel().test(this) {
                        startAndAwaitCards("JPY")

                        // EUR 조회가 끝나지 않은 채 멈춰 있다.
                        favoritesFlow.value = listOf("JPY", "EUR")
                        awaitFavoriteRates { it.isLoading }

                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("2.5"))

                        val whileLoading = awaitFavoriteRates { it.items.singleOrNull()?.convertedAmount == "23.75" }
                        whileLoading.isLoading shouldBe true
                        whileLoading.items.single().rateLabel shouldBe "1 USD = 9.5000 JPY"

                        usdEur.complete(0.8)

                        val loaded = awaitFavoriteRates { !it.isLoading }
                        loaded.codes() shouldContainExactly listOf("JPY", "EUR")
                        loaded.items.map { it.convertedAmount } shouldContainExactly listOf("23.75", "2.00")
                        loaded.items.map { it.rateLabel } shouldContainExactly listOf(
                            "1 USD = 9.5000 JPY",
                            "1 USD = 0.8000 EUR",
                        )
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("기준 통화를 바꿔 재조회가 끝나지 않은 동안 금액을 바꾸면") {
            Then("옛 기준 통화의 라벨과 환율로 카드 금액을 바꾸고, 새 기준 통화 라벨에 옛 환율을 섞지 않는다") {
                runTest {
                    val eurJpy = CompletableDeferred<Double>()
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5
                    coEvery { getExchangeRateUseCase("EUR", "JPY") } coAnswers { eurJpy.await() }

                    createViewModel().test(this) {
                        startAndAwaitCards("JPY")
                        val seen = mutableListOf<ExchangeContract.FavoriteRates>()

                        // EUR 기준 조회가 끝나지 않은 채 멈춰 있다.
                        containerHost.processIntent(ExchangeContract.Intent.SelectFromCurrency(eur))
                        awaitFavoriteRates(seen) { it.isLoading }

                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("2.5"))

                        val whileLoading = awaitFavoriteRates(seen) {
                            it.items.singleOrNull()?.convertedAmount == "23.75"
                        }
                        whileLoading.isLoading shouldBe true
                        whileLoading.items.single().rateLabel shouldBe "1 USD = 9.5000 JPY"

                        eurJpy.complete(160.0)

                        val loaded = awaitFavoriteRates(seen) { !it.isLoading }
                        loaded.items.single().convertedAmount shouldBe "400.00"
                        loaded.items.single().rateLabel shouldBe "1 EUR = 160.0000 JPY"

                        // 지나간 카드 중 EUR 라벨이 붙은 것은 EUR 환율(160)로 만든 것뿐이어야 한다.
                        seen.flatMap { it.items }
                            .map { it.rateLabel }
                            .filter { it.startsWith("1 EUR ") }
                            .distinct() shouldContainExactly listOf("1 EUR = 160.0000 JPY")
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }

        When("즐겨찾기를 모두 지운 뒤 다시 더해 재조회가 끝나지 않은 동안 금액을 바꾸면") {
            Then("지운 카드를 되살리지 않고, 조회가 끝나면 새 즐겨찾기 카드만 보여 준다") {
                runTest {
                    val usdEur = CompletableDeferred<Double>()
                    favoritesFlow.value = listOf("JPY")
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5
                    coEvery { getExchangeRateUseCase("USD", "EUR") } coAnswers { usdEur.await() }

                    createViewModel().test(this) {
                        startAndAwaitCards("JPY")

                        favoritesFlow.value = emptyList()
                        awaitFavoriteRates { it.items.isEmpty() } shouldBe ExchangeContract.FavoriteRates()

                        // EUR 조회가 끝나지 않은 채 멈춰 있다.
                        favoritesFlow.value = listOf("EUR")
                        awaitFavoriteRates { it.isLoading } shouldBe ExchangeContract.FavoriteRates(isLoading = true)

                        containerHost.processIntent(ExchangeContract.Intent.UpdateFromAmount("10"))
                        awaitItemsUntil { it.fromAmount == "10" }
                        runCurrentScheduler()

                        currentFavoriteRates() shouldBe ExchangeContract.FavoriteRates(isLoading = true)

                        usdEur.complete(0.8)

                        val loaded = awaitFavoriteRates { !it.isLoading }
                        loaded.codes() shouldContainExactly listOf("EUR")
                        loaded.items.single().convertedAmount shouldBe "8.00"
                        cancelAndIgnoreRemainingItems()
                    }
                }
            }
        }
    }
})

/** 상태 방출을 차례로 꺼내 조건을 만족하는 상태를 돌려준다. 부수 효과(스낵바)는 건너뛴다. */
private suspend fun ExchangeTestContext.awaitItemsUntil(
    predicate: (ExchangeContract.State) -> Boolean,
): ExchangeContract.State {
    while (true) {
        val item = awaitItem()
        if (item is Item.StateItem && predicate(item.value)) {
            return item.value
        }
    }
}
