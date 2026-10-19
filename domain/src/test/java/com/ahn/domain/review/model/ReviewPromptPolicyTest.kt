package com.ahn.domain.review.model

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Duration

class ReviewPromptPolicyTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val now = Duration.ofDays(1_000).toMillis()
        val eligibleRecord = ReviewPromptRecord(
            firstOpenedAt = now - Duration.ofDays(3).toMillis(),
            lastOpenedEpochDay = 1_000,
            openedDayCount = 3,
            calculationSuccessCount = 5,
            lastRequestedAt = null,
        )

        Given("모든 조건을 정확히 경계값으로 만족하면") {
            When("요청 여부를 판단하면") {
                Then("요청해야 한다") {
                    ReviewPromptPolicy.shouldRequest(eligibleRecord, now) shouldBe true
                }
            }
        }

        Given("첫 실행 기록이 없으면") {
            When("요청 여부를 판단하면") {
                Then("요청하지 않아야 한다") {
                    val record = eligibleRecord.copy(firstOpenedAt = null)

                    ReviewPromptPolicy.shouldRequest(record, now) shouldBe false
                }
            }
        }

        Given("첫 실행 후 3일이 지나지 않았으면") {
            When("요청 여부를 판단하면") {
                Then("요청하지 않아야 한다") {
                    val record = eligibleRecord.copy(firstOpenedAt = now - Duration.ofDays(3).toMillis() + 1)

                    ReviewPromptPolicy.shouldRequest(record, now) shouldBe false
                }
            }
        }

        Given("앱을 연 날이 3일보다 적으면") {
            When("요청 여부를 판단하면") {
                Then("요청하지 않아야 한다") {
                    val record = eligibleRecord.copy(openedDayCount = 2)

                    ReviewPromptPolicy.shouldRequest(record, now) shouldBe false
                }
            }
        }

        Given("계산 성공이 5회보다 적으면") {
            When("요청 여부를 판단하면") {
                Then("요청하지 않아야 한다") {
                    val record = eligibleRecord.copy(calculationSuccessCount = 4)

                    ReviewPromptPolicy.shouldRequest(record, now) shouldBe false
                }
            }
        }

        Given("마지막 요청 후 120일이 지나지 않았으면") {
            When("요청 여부를 판단하면") {
                Then("요청하지 않아야 한다") {
                    val record = eligibleRecord.copy(lastRequestedAt = now - Duration.ofDays(120).toMillis() + 1)

                    ReviewPromptPolicy.shouldRequest(record, now) shouldBe false
                }
            }
        }

        Given("마지막 요청 후 120일이 지났으면") {
            When("요청 여부를 판단하면") {
                Then("다시 요청해야 한다") {
                    val record = eligibleRecord.copy(lastRequestedAt = now - Duration.ofDays(120).toMillis())

                    ReviewPromptPolicy.shouldRequest(record, now) shouldBe true
                }
            }
        }
    })
