package com.ahn.presentation.ui.screen.exchange

import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.domain.exchange.usecase.ExchangeUseCases
import com.ahn.domain.favorite.usecase.FavoriteUseCases
import kotlinx.coroutines.CancellationException
import java.util.Locale

/**
 * 즐겨찾기 카드를 만드는 데 필요한 환율 상태. 이 값이 바뀔 때만 환율을 다시 모은다.
 * 금액(`fromAmount`)은 들어 있지 않다. 금액만 바뀌면 이미 모은 환율로 카드만 다시 만든다.
 */
internal data class FavoriteRateInput(
    val fromCurrency: CurrencyInfo?,
    val favoriteCurrencyCodes: List<String>,
    val availableCurrencies: List<CurrencyInfo>,
    val exchangeRateDate: String,
    val exchangeRateFetchedAt: Long,
) {
    companion object {
        fun from(state: ExchangeContract.State) = FavoriteRateInput(
            fromCurrency = state.fromCurrency,
            favoriteCurrencyCodes = state.favoriteCurrencyCodes,
            availableCurrencies = state.availableCurrencies,
            exchangeRateDate = state.exchangeRateDate,
            exchangeRateFetchedAt = state.exchangeRateFetchedAt,
        )
    }
}

/**
 * 조회를 마친 입력과 그 입력으로 모은 환율. 둘을 늘 함께 쓴다.
 * 환율은 [input]의 기준 통화 대비 값이라, 다른 입력과 섞으면 기준 통화가 다른 틀린 카드가 나온다.
 */
internal data class LoadedFavoriteRates(
    val input: FavoriteRateInput,
    val ratesByCode: Map<String, Double>,
)

/**
 * 즐겨찾기 통화의 환율을 모으고 카드로 바꾼다. 상태를 들지 않는다.
 * 언제 다시 모을지는 [ExchangeViewModel]이 정한다.
 */
internal class FavoriteRatesLoader(
    private val exchangeUseCases: ExchangeUseCases,
    private val favoriteUseCases: FavoriteUseCases,
) {
    /**
     * 환율을 모을 필요가 있는지 판단한다.
     * 기준 통화는 카드로 만들지 않으므로, 즐겨찾기가 비었거나 기준 통화뿐이면 조회할 환율이 없다.
     */
    fun needsRates(input: FavoriteRateInput): Boolean {
        val base = input.fromCurrency ?: return false
        if (input.availableCurrencies.isEmpty()) return false
        return !input.favoriteCurrencyCodes.all { it == base.code }
    }

    /** 조회할 환율이 없을 때 보여 줄 상태. 즐겨찾기가 기준 통화뿐이면 그 코드를 담는다. */
    fun emptyRates(input: FavoriteRateInput): ExchangeContract.FavoriteRates {
        return ExchangeContract.FavoriteRates(
            isLoading = false,
            items = emptyList(),
            baseOnlyFavoriteCode = baseOnlyFavoriteCode(input),
        )
    }

    /**
     * 즐겨찾기 순서대로 기준 통화 대비 환율을 모은다.
     * 기준 통화와 통화 목록에 없는 코드는 건너뛰고, 조회에 실패한 코드는 결과에서 뺀다.
     */
    suspend fun loadRates(input: FavoriteRateInput): Map<String, Double> {
        val base = input.fromCurrency ?: return emptyMap()
        val availableCodes = input.availableCurrencies.mapTo(mutableSetOf()) { it.code }
        val rates = mutableMapOf<String, Double>()

        for (code in input.favoriteCurrencyCodes) {
            if (code == base.code) continue
            if (code !in availableCodes) continue

            // 실패한 통화는 카드에서 뺀다. 화면의 실패 표시는 환율 상태(failure)가 맡는다.
            runCatching {
                exchangeUseCases.getExchangeRate(base.code, code)
            }.onSuccess { rate ->
                rates[code] = rate
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }

        return rates
    }

    /** 모은 환율과 기준 금액으로 카드를 만든다. */
    fun buildItems(
        input: FavoriteRateInput,
        baseAmount: String,
        ratesByCode: Map<String, Double>,
    ): List<ExchangeContract.FavoriteRateItem> {
        val favoriteRates = favoriteUseCases.buildFavoriteRates(
            baseCurrency = input.fromCurrency,
            baseAmount = baseAmount,
            favoriteCurrencyCodes = input.favoriteCurrencyCodes,
            availableCurrencies = input.availableCurrencies,
            ratesByCode = ratesByCode,
        )

        return favoriteRates.map { rateInfo ->
            ExchangeContract.FavoriteRateItem(
                currency = rateInfo.currency,
                convertedAmount = String.format(Locale.US, "%.2f", rateInfo.convertedAmount),
                rateLabel = "1 ${rateInfo.baseCurrencyCode} = ${
                    String.format(Locale.US, "%.4f", rateInfo.rate)
                } ${rateInfo.currency.code}",
            )
        }
    }

    private fun baseOnlyFavoriteCode(input: FavoriteRateInput): String? {
        val base = input.fromCurrency ?: return null
        if (input.favoriteCurrencyCodes.isEmpty()) return null
        return base.code.takeIf { input.favoriteCurrencyCodes.all { it == base.code } }
    }
}
