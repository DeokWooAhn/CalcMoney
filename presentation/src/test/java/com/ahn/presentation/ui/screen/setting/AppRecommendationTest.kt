package com.ahn.presentation.ui.screen.setting

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.net.URI
import java.net.URLDecoder

class AppRecommendationTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        Given("앱 패키지 이름이 주어지면") {
            When("추천 링크를 만들면") {
                val url = playStoreRecommendUrl("com.ahn.calcmoney")
                val query = URI(url).rawQuery
                    .split("&")
                    .associate { it.substringBefore("=") to it.substringAfter("=") }

                Then("그 앱의 Play 스토어 상세 페이지를 가리켜야 한다") {
                    url.substringBefore("?") shouldBe "https://play.google.com/store/apps/details"
                    query["id"] shouldBe "com.ahn.calcmoney"
                }

                Then("referrer를 풀면 공유 유입을 구분하는 UTM이 나와야 한다") {
                    URLDecoder.decode(query.getValue("referrer"), Charsets.UTF_8) shouldBe "utm_source=app_share"
                }
            }
        }
    })
