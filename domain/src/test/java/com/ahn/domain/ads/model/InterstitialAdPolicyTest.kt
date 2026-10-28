package com.ahn.domain.ads.model

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class InterstitialAdPolicyTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val today = 1_000L
        val now = Duration.ofDays(today).toMillis() + Duration.ofHours(12).toMillis()

        // 모든 조건을 정확히 경계값으로 만족하는 기록
        val eligibleRecord = InterstitialAdRecord(
            firstCalculatedAt = now - Duration.ofHours(24).toMillis(),
            calculationCountSinceLastAd = 4,
            lastShownAt = now - Duration.ofMinutes(5).toMillis(),
            lastShownEpochDay = today,
            shownCountOfDay = 2,
        )

        fun shouldShow(record: InterstitialAdRecord) = InterstitialAdPolicy.shouldShow(record, now, today)

        Given("모든 조건을 정확히 경계값으로 만족하면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 줘야 한다") {
                    shouldShow(eligibleRecord) shouldBe true
                }
            }
        }

        Given("광고를 한 번도 보여 주지 않았고 유예 기간과 계산 횟수를 채웠으면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 줘야 한다") {
                    val record = eligibleRecord.copy(lastShownAt = null, lastShownEpochDay = null, shownCountOfDay = 0)

                    shouldShow(record) shouldBe true
                }
            }
        }

        Given("계산한 기록이 없으면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 주지 않아야 한다") {
                    shouldShow(InterstitialAdRecord()) shouldBe false
                }
            }
        }

        Given("처음 계산하고 24시간이 지나지 않았으면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 주지 않아야 한다") {
                    val record = eligibleRecord.copy(firstCalculatedAt = now - Duration.ofHours(24).toMillis() + 1)

                    shouldShow(record) shouldBe false
                }
            }
        }

        Given("마지막 광고 뒤로 계산이 4번보다 적으면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 주지 않아야 한다") {
                    shouldShow(eligibleRecord.copy(calculationCountSinceLastAd = 3)) shouldBe false
                }
            }
        }

        Given("마지막 광고 뒤로 5분이 지나지 않았으면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 주지 않아야 한다") {
                    val record = eligibleRecord.copy(lastShownAt = now - Duration.ofMinutes(5).toMillis() + 1)

                    shouldShow(record) shouldBe false
                }
            }
        }

        Given("오늘 이미 3번 보여 줬으면") {
            When("광고를 보여 줄지 판단하면") {
                Then("보여 주지 않아야 한다") {
                    shouldShow(eligibleRecord.copy(shownCountOfDay = 3)) shouldBe false
                }
            }
        }

        Given("어제 3번 보여 줬으면") {
            When("오늘 광고를 보여 줄지 판단하면") {
                Then("오늘 횟수는 처음부터 세므로 보여 줘야 한다") {
                    val record = eligibleRecord.copy(lastShownEpochDay = today - 1, shownCountOfDay = 3)

                    shouldShow(record) shouldBe true
                }
            }
        }
    })
