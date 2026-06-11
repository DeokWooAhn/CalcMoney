package com.ahn.presentation.util

import com.ahn.domain.exchange.model.ExchangeRateException
import com.ahn.presentation.R
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ExchangeRateErrorMapperTest :
    DescribeSpec({
        describe("환율 에러 메시지 변환") {
            it("아직 고시되지 않은 환율은 11시 이후 확인 안내로 변환한다") {
                ExchangeRateException.NotReady().toExchangeRateErrorUiText() shouldBe
                    UiText.StringResource(R.string.exchange_rate_not_ready)
            }

            it("네트워크 오류는 연결 상태 확인 안내로 변환한다") {
                ExchangeRateException.NetworkUnavailable().toExchangeRateErrorUiText() shouldBe
                    UiText.StringResource(R.string.exchange_rate_network_unavailable)
            }

            it("선택 통화의 환율이 없으면 통화 환율 없음 안내로 변환한다") {
                ExchangeRateException.RateNotFound("USD").toExchangeRateErrorUiText() shouldBe
                    UiText.StringResource(R.string.exchange_rate_not_found)
            }

            it("일시적으로 사용할 수 없는 환율은 잠시 후 다시 시도 안내로 변환한다") {
                ExchangeRateException.TemporarilyUnavailable().toExchangeRateErrorUiText() shouldBe
                    UiText.StringResource(R.string.exchange_rate_temporarily_unavailable)
            }

            it("알 수 없는 오류는 잠시 후 다시 시도 안내로 변환한다") {
                IllegalStateException("internal error").toExchangeRateErrorUiText() shouldBe
                    UiText.StringResource(R.string.exchange_rate_temporarily_unavailable)
            }
        }
    })
