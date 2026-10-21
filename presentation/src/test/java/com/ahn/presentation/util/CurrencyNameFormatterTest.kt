package com.ahn.presentation.util

import android.content.res.Resources
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.util.Locale

class CurrencyNameFormatterTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        // 직접 다듬은 번역은 문자열 리소스에서 온다. 기기 언어에 맞는 값이 들어 있다고 본다.
        val resources = mockk<Resources> {
            every { getString(R.string.currency_name_usd) } returns "다듬은 미국 달러"
        }
        val usd = CurrencyInfo(code = "USD", displayCode = "USD", name = "미국 달러(서버)", flagEmoji = "🇺🇸")
        val twd = CurrencyInfo(code = "TWD", displayCode = "TWD", name = "신 타이완 달러(서버)", flagEmoji = "🇹🇼")
        val ggp = CurrencyInfo(code = "GGP", displayCode = "GGP", name = "건지 파운드(서버)", flagEmoji = "🇬🇬")

        Given("한국어·영어 기기에서") {
            When("다듬은 번역이 있는 통화의 이름을 만들면") {
                Then("다듬은 번역을 써야 한다") {
                    usd.localizedName(resources, Locale.KOREAN) shouldBe "다듬은 미국 달러"
                    usd.localizedName(resources, Locale.US) shouldBe "다듬은 미국 달러"
                }
            }

            When("다듬은 번역이 없는 통화의 이름을 만들면") {
                Then("플랫폼 이름을 써야 한다") {
                    twd.localizedName(resources, Locale.ENGLISH) shouldBe platformCurrencyName("TWD", Locale.ENGLISH)
                }
            }
        }

        Given("다듬은 번역이 없는 언어(프랑스어) 기기에서") {
            When("다듬은 번역이 있는 통화의 이름을 만들면") {
                val name = usd.localizedName(resources, Locale.FRENCH)

                Then("목록 안에서 언어가 섞이지 않게 플랫폼의 프랑스어 이름을 써야 한다") {
                    name shouldBe platformCurrencyName("USD", Locale.FRENCH)
                    name shouldNotBe "다듬은 미국 달러"
                }
            }

            When("플랫폼도 모르는 통화의 이름을 만들면") {
                Then("서버가 준 이름을 써야 한다") {
                    ggp.localizedName(resources, Locale.FRENCH) shouldBe "건지 파운드(서버)"
                }
            }
        }
    })
