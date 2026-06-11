package com.ahn.domain.exchange.model

sealed class ExchangeRateException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class NotReady(cause: Throwable? = null) : ExchangeRateException(
        message = "Exchange rate data is not ready.",
        cause = cause,
    )

    class NetworkUnavailable(cause: Throwable? = null) : ExchangeRateException(
        message = "Exchange rate network is unavailable.",
        cause = cause,
    )

    class TemporarilyUnavailable(cause: Throwable? = null) : ExchangeRateException(
        message = "Exchange rate data is temporarily unavailable.",
        cause = cause,
    )

    class RateNotFound(val currencyCode: String) : ExchangeRateException(
        message = "Exchange rate data not found for $currencyCode.",
    )
}
