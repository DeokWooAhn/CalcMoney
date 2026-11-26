package com.ahn.presentation.ui.screen.exchange

import com.ahn.domain.currency.model.CurrencyInfo
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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest

class FavoriteRatesLoaderTest : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val usd = CurrencyInfo("USD", "USD", "미국 달러", "🇺🇸")
    val krw = CurrencyInfo("KRW", "KRW", "대한민국 원", "🇰🇷")
    val jpy = CurrencyInfo("JPY", "JPY", "일본 엔", "🇯🇵")
    val currencies = listOf(usd, krw, jpy)

    val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>()

    fun createLoader() = FavoriteRatesLoader(
        exchangeUseCases = ExchangeUseCases(
            exchangeAmount = mockk<CalculateExchangeAmountUseCase>(),
            convertExchangeAmount = mockk<ConvertExchangeAmountUseCase>(),
            getExchangeRate = getExchangeRateUseCase,
            getLatestRateDate = mockk<GetLatestExchangeRateDateUseCase>(),
            getLatestFetchedAt = mockk<GetLatestExchangeRateFetchedAtUseCase>(),
            refreshExchangeRates = mockk<RefreshExchangeRatesUseCase>(),
            getSupportedCurrencies = mockk<GetSupportedCurrenciesUseCase>(),
        ),
        favoriteUseCases = FavoriteUseCases(
            buildFavoriteRates = BuildFavoriteRatesUseCase(),
            getFavoriteCurrencies = mockk<GetFavoriteCurrenciesUseCase>(),
            toggleFavoriteCurrency = mockk<ToggleFavoriteCurrencyUseCase>(),
        ),
    )

    fun input(
        fromCurrency: CurrencyInfo?,
        favoriteCurrencyCodes: List<String>,
        availableCurrencies: List<CurrencyInfo> = currencies,
    ) = FavoriteRateInput(
        fromCurrency = fromCurrency,
        favoriteCurrencyCodes = favoriteCurrencyCodes,
        availableCurrencies = availableCurrencies,
        exchangeRateDate = "",
        exchangeRateFetchedAt = 0L,
    )

    beforeEach { clearAllMocks() }

    Given("즐겨찾기 환율 로더에서") {
        When("기준 통화가 없으면") {
            Then("조회할 필요가 없고, 빈 목록 상태를 만들며 환율을 조회하지 않아야 한다") {
                runTest {
                    val loader = createLoader()
                    val noBase = input(fromCurrency = null, favoriteCurrencyCodes = listOf("KRW"))

                    loader.needsRates(noBase) shouldBe false
                    loader.emptyRates(noBase) shouldBe ExchangeContract.FavoriteRates(
                        isLoading = false,
                        items = emptyList(),
                    )
                    loader.loadRates(noBase).shouldBeEmpty()
                    coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
                }
            }
        }

        When("통화 목록이 비어 있으면") {
            Then("조회할 필요가 없어야 한다") {
                val loader = createLoader()

                loader.needsRates(
                    input(fromCurrency = usd, favoriteCurrencyCodes = listOf("KRW"), availableCurrencies = emptyList()),
                ) shouldBe false
            }
        }

        When("즐겨찾기 통화의 환율을 가져오면") {
            Then("환산 금액과 환율 라벨을 즐겨찾기 순서대로 만들어야 한다") {
                runTest {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1500.0
                    coEvery { getExchangeRateUseCase("USD", "JPY") } returns 9.5

                    val loader = createLoader()
                    val favorites = input(fromCurrency = usd, favoriteCurrencyCodes = listOf("KRW", "JPY"))

                    loader.needsRates(favorites) shouldBe true
                    val rates = loader.loadRates(favorites)
                    val items = loader.buildItems(favorites, baseAmount = "1", ratesByCode = rates)

                    items.map { it.currency.code } shouldContainExactly listOf("KRW", "JPY")
                    items[0].convertedAmount shouldBe "1,500.00"
                    items[0].rateLabel shouldBe "1 USD = 1500.0000 KRW"
                    items[1].convertedAmount shouldBe "9.50"
                    items[1].rateLabel shouldBe "1 USD = 9.5000 JPY"
                }
            }
        }

        When("즐겨찾기에 기준 통화, 없는 통화, 실패한 통화가 섞여 있으면") {
            Then("성공적으로 조회된 통화만 목록에 보여야 한다") {
                runTest {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } returns 1500.0
                    coEvery { getExchangeRateUseCase("USD", "JPY") } throws IllegalStateException("rate not found")

                    val loader = createLoader()
                    val mixed = input(fromCurrency = usd, favoriteCurrencyCodes = listOf("USD", "KRW", "JPY", "EUR"))

                    val rates = loader.loadRates(mixed)
                    val items = loader.buildItems(mixed, baseAmount = "1", ratesByCode = rates)

                    items.map { it.currency.code } shouldContainExactly listOf("KRW")
                    items.single().convertedAmount shouldBe "1,500.00"
                    coVerify(exactly = 0) { getExchangeRateUseCase("USD", "USD") }
                    coVerify(exactly = 1) { getExchangeRateUseCase("USD", "KRW") }
                    coVerify(exactly = 1) { getExchangeRateUseCase("USD", "JPY") }
                    coVerify(exactly = 0) { getExchangeRateUseCase("USD", "EUR") }
                }
            }
        }

        When("즐겨찾기가 기준 통화 하나뿐이면") {
            Then("조회할 필요가 없고, 로드 실패가 아니라 기준 통화만 즐겨찾기된 상태를 만들어야 한다") {
                val loader = createLoader()
                val baseOnly = input(fromCurrency = usd, favoriteCurrencyCodes = listOf("USD"))

                loader.needsRates(baseOnly) shouldBe false
                loader.emptyRates(baseOnly) shouldBe ExchangeContract.FavoriteRates(
                    isLoading = false,
                    items = emptyList(),
                    baseOnlyFavoriteCode = "USD",
                )
            }
        }

        When("기준 통화 외의 즐겨찾기가 있지만 환율 조회가 모두 실패하면") {
            Then("조회는 필요하다고 판단하고(안내 상태가 아님), 만들어진 카드는 없어야 한다") {
                runTest {
                    coEvery { getExchangeRateUseCase("USD", "KRW") } throws IllegalStateException("rate not found")

                    val loader = createLoader()
                    val allFailed = input(fromCurrency = usd, favoriteCurrencyCodes = listOf("USD", "KRW"))

                    // 조회가 필요한 입력이면 ViewModel은 baseOnlyFavoriteCode를 null로 둔다.
                    loader.needsRates(allFailed) shouldBe true
                    val rates = loader.loadRates(allFailed)
                    rates.shouldBeEmpty()
                    loader.buildItems(allFailed, baseAmount = "1", ratesByCode = rates).shouldBeEmpty()
                }
            }
        }

        When("즐겨찾기가 비어 있으면") {
            Then("조회할 필요가 없고 안내 상태도 만들지 않아야 한다") {
                val loader = createLoader()
                val empty = input(fromCurrency = usd, favoriteCurrencyCodes = emptyList())

                loader.needsRates(empty) shouldBe false
                loader.emptyRates(empty) shouldBe ExchangeContract.FavoriteRates()
            }
        }
    }
})
