package com.ahn.presentation.util

import com.ahn.domain.exchange.model.ExchangeRateException
import com.ahn.presentation.R

fun Throwable.toExchangeRateErrorUiText(): UiText {
    return when (this) {
        is ExchangeRateException.NotReady -> UiText.StringResource(R.string.exchange_rate_not_ready)
        is ExchangeRateException.NetworkUnavailable -> UiText.StringResource(R.string.exchange_rate_network_unavailable)
        is ExchangeRateException.RateNotFound -> UiText.StringResource(R.string.exchange_rate_not_found)
        is ExchangeRateException.TemporarilyUnavailable ->
            UiText.StringResource(R.string.exchange_rate_temporarily_unavailable)

        else -> UiText.StringResource(R.string.exchange_rate_temporarily_unavailable)
    }
}
