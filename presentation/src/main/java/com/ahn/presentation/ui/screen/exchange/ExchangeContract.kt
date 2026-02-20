package com.ahn.presentation.ui.screen.exchange

interface ExchangeContract {
    data class State(
        val fromAmount: String = "1",
        val toAmount: String = "",
        val fromCurrency: Currency = Currency.USD,
        val toCurrency: Currency = Currency.KRW,
        val exchangeRate: Double = 0.0,
        val isLoading: Boolean = false,
    )

    sealed interface Intent {
        data class UpdateFromAmount(val amount: String) : Intent
        data class SelectFromCurrency(val currency: Currency) : Intent
        data class SelectToCurrency(val currency: Currency) : Intent
        object SwapCurrencies : Intent
    }

    sealed interface SideEffect {
        data class ShowSnackBar(val message: String) : SideEffect
    }
}

// 통화 정보
enum class Currency(
    val code: String,
    val currencyName: String,
    val flagEmoji: String // 또는 drawable 리소스 ID
) {
    KRW("KRW", "대한민국 원", "🇰🇷"),
    USD("USD", "미국 달러", "🇺🇸"),
    JPY("JPY", "일본 엔", "🇯🇵"),
    EUR("EUR", "유로", "🇪🇺"),
    CNY("CNY", "중국 위안", "🇨🇳"),
    GBP("GBP", "영국 파운드", "🇬🇧"),
    AUD("AUD", "호주 달러", "🇦🇺"),
    CAD("CAD", "캐나다 달러", "🇨🇦"),
    CHF("CHF", "스위스 프랑", "🇨🇭"),
    HKD("HKD", "홍콩 달러", "🇭🇰");
}