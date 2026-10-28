package com.ahn.presentation.util

import android.content.res.Resources
import com.ahn.domain.currency.model.CurrencyInfo
import com.ahn.presentation.R
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.util.Locale

class CurrencyNameFormatterTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        // 직접 다듬은 번역과 예비 이름은 문자열 리소스에서 온다. 기기 언어에 맞는 값이 들어 있다고 본다.
        val resources = mockk<Resources> {
            every { getString(R.string.currency_name_usd) } returns "다듬은 미국 달러"
            every { getString(R.string.currency_name_ggp) } returns "예비 건지 파운드"
            every { getString(R.string.currency_name_mru) } returns "예비 모리타니 우기야"
        }
        val usd = CurrencyInfo(code = "USD", displayCode = "USD", name = "미국 달러(서버)", flagEmoji = "🇺🇸")
        val twd = CurrencyInfo(code = "TWD", displayCode = "TWD", name = "신 타이완 달러(서버)", flagEmoji = "🇹🇼")
        val ggp = CurrencyInfo(code = "GGP", displayCode = "GGP", name = "건지 파운드(서버)", flagEmoji = "🇬🇬")
        val mru = CurrencyInfo(code = "MRU", displayCode = "MRU", name = "모리타니 우기야(서버)", flagEmoji = "🇲🇷")
        val unknown = CurrencyInfo(code = "QQQ", displayCode = "QQQ", name = "새 통화(서버)", flagEmoji = "🌐")

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

            When("플랫폼이 모르는 지역 통화의 이름을 만들면") {
                val name = ggp.localizedName(resources, Locale.ENGLISH)

                Then("영어 기기에서도 서버의 한국어 이름 대신 예비 이름을 써야 한다") {
                    name shouldBe "예비 건지 파운드"
                    name shouldNotBe "건지 파운드(서버)"
                }
            }

            When("예비 이름이 있지만 플랫폼이 아는 통화의 이름을 만들면") {
                val platformName = platformCurrencyName("MRU", Locale.ENGLISH)

                Then("플랫폼 이름을 써야 한다") {
                    platformName.shouldNotBeNull()
                    mru.localizedName(resources, Locale.ENGLISH) shouldBe platformName
                }
            }

            When("어디에도 이름이 없는 통화의 이름을 만들면") {
                Then("한국어 기기에서는 서버 이름을 써야 한다") {
                    unknown.localizedName(resources, Locale.KOREAN) shouldBe "새 통화(서버)"
                }

                Then("영어 기기에서는 한국어가 섞이지 않게 통화 코드를 써야 한다") {
                    unknown.localizedName(resources, Locale.ENGLISH) shouldBe "QQQ"
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
                Then("서버의 한국어 이름 대신 예비 이름을 써야 한다") {
                    ggp.localizedName(resources, Locale.FRENCH) shouldBe "예비 건지 파운드"
                }
            }

            When("어디에도 이름이 없는 통화의 이름을 만들면") {
                Then("서버의 한국어 이름 대신 통화 코드를 써야 한다") {
                    unknown.localizedName(resources, Locale.FRENCH) shouldBe "QQQ"
                }
            }
        }
    })
