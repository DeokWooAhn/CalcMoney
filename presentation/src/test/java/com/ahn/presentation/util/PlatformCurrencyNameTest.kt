package com.ahn.presentation.util

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldNotBe
import java.util.Locale

class PlatformCurrencyNameTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        Given("플랫폼이 아는 ISO 4217 통화일 때") {
            When("한국어 로케일로 이름을 찾으면") {
                val name = platformCurrencyName("TWD", Locale.KOREAN)

                Then("통화 코드가 아닌 이름을 반환해야 한다") {
                    name.shouldNotBeNull()
                    name shouldNotBe "TWD"
                }
            }

            When("소문자 코드로 찾으면") {
                Then("대소문자와 관계없이 이름을 반환해야 한다") {
                    platformCurrencyName("vnd", Locale.ENGLISH).shouldNotBeNull()
                }
            }
        }

        Given("ISO 4217에 없는 지역 통화일 때") {
            When("이름을 찾으면") {
                Then("서버 이름을 쓰도록 null을 반환해야 한다") {
                    platformCurrencyName("GGP", Locale.KOREAN).shouldBeNull()
                }
            }
        }

        Given("형식이 잘못된 코드일 때") {
            When("이름을 찾으면") {
                Then("예외 없이 null을 반환해야 한다") {
                    platformCurrencyName("NOT-A-CODE", Locale.KOREAN).shouldBeNull()
                }
            }
        }
    })
