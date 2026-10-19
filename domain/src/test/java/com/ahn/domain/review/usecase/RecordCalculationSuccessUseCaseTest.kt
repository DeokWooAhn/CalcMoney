package com.ahn.domain.review.usecase

import com.ahn.domain.review.FakeReviewPromptRepository
import com.ahn.domain.review.model.ReviewPromptRecord
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RecordCalculationSuccessUseCaseTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val clock = Clock.fixed(Instant.parse("2026-10-20T01:00:00Z"), ZoneOffset.UTC)
        val now = clock.millis()

        // 계산 성공 1회만 더하면 요청 조건을 모두 만족하는 기록
        val almostEligibleRecord = ReviewPromptRecord(
            firstOpenedAt = now - Duration.ofDays(10).toMillis(),
            lastOpenedEpochDay = 0,
            openedDayCount = 3,
            calculationSuccessCount = 4,
        )

        Given("요청 조건을 아직 만족하지 못한 상태에서") {
            When("계산 성공을 기록하면") {
                Then("횟수만 늘리고 요청하지 않아야 한다") {
                    val repository = FakeReviewPromptRepository(
                        almostEligibleRecord.copy(calculationSuccessCount = 2),
                    )

                    val shouldRequest = RecordCalculationSuccessUseCase(repository, clock)()

                    shouldRequest shouldBe false
                    repository.record.calculationSuccessCount shouldBe 3
                    repository.record.lastRequestedAt shouldBe null
                }
            }
        }

        Given("이번 계산으로 요청 조건을 만족하게 되는 상태에서") {
            When("계산 성공을 기록하면") {
                Then("요청해야 하고 요청 시각을 남겨야 한다") {
                    val repository = FakeReviewPromptRepository(almostEligibleRecord)

                    val shouldRequest = RecordCalculationSuccessUseCase(repository, clock)()

                    shouldRequest shouldBe true
                    repository.record.calculationSuccessCount shouldBe 5
                    repository.record.lastRequestedAt shouldBe now
                }
            }
        }

        Given("방금 리뷰를 요청한 상태에서") {
            When("다음 계산 성공을 기록하면") {
                Then("다시 요청하지 않아야 한다") {
                    val repository = FakeReviewPromptRepository(almostEligibleRecord)
                    val useCase = RecordCalculationSuccessUseCase(repository, clock)

                    useCase()
                    val shouldRequestAgain = useCase()

                    shouldRequestAgain shouldBe false
                    repository.record.calculationSuccessCount shouldBe 6
                    repository.record.lastRequestedAt shouldBe now
                }
            }
        }
    })
