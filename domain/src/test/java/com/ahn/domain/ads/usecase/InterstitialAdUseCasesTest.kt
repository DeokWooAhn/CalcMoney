package com.ahn.domain.ads.usecase

import com.ahn.domain.ads.FakeInterstitialAdRepository
import com.ahn.domain.ads.model.InterstitialAdRecord
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class InterstitialAdUseCasesTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val clock = Clock.fixed(Instant.parse("2026-10-27T03:00:00Z"), ZoneOffset.UTC)
        val now = clock.millis()
        val today = LocalDate.now(clock).toEpochDay()

        Given("계산한 적이 없는 상태에서") {
            When("계산 성공을 기록하면") {
                Then("처음 계산한 시각을 남기고 횟수를 1로 해야 한다") {
                    val repository = FakeInterstitialAdRepository()

                    RecordInterstitialAdCalculationUseCase(repository, clock)()

                    repository.record.firstCalculatedAt shouldBe now
                    repository.record.calculationCountSinceLastAd shouldBe 1
                }
            }
        }

        Given("이미 계산한 적이 있는 상태에서") {
            When("계산 성공을 기록하면") {
                Then("처음 계산한 시각은 그대로 두고 횟수만 늘려야 한다") {
                    val firstCalculatedAt = now - Duration.ofDays(2).toMillis()
                    val repository = FakeInterstitialAdRepository(
                        InterstitialAdRecord(firstCalculatedAt = firstCalculatedAt, calculationCountSinceLastAd = 3),
                    )

                    RecordInterstitialAdCalculationUseCase(repository, clock)()

                    repository.record.firstCalculatedAt shouldBe firstCalculatedAt
                    repository.record.calculationCountSinceLastAd shouldBe 4
                }
            }
        }

        Given("광고 조건을 모두 채운 상태에서") {
            val eligibleRecord = InterstitialAdRecord(
                firstCalculatedAt = now - Duration.ofDays(2).toMillis(),
                calculationCountSinceLastAd = 4,
            )

            When("광고를 보여 줄지 물으면") {
                Then("보여 줘야 하고 기록은 바꾸지 않아야 한다") {
                    val repository = FakeInterstitialAdRepository(eligibleRecord)

                    ShouldShowInterstitialAdUseCase(repository, clock)() shouldBe true
                    repository.record shouldBe eligibleRecord
                }
            }

            When("광고를 보여 줬다고 기록하면") {
                Then("계산 횟수를 0으로 되돌리고 보여 준 시각과 오늘 횟수를 남겨야 한다") {
                    val repository = FakeInterstitialAdRepository(eligibleRecord)

                    RecordInterstitialAdShownUseCase(repository, clock)()

                    repository.record.calculationCountSinceLastAd shouldBe 0
                    repository.record.lastShownAt shouldBe now
                    repository.record.lastShownEpochDay shouldBe today
                    repository.record.shownCountOfDay shouldBe 1
                    ShouldShowInterstitialAdUseCase(repository, clock)() shouldBe false
                }
            }
        }

        Given("오늘 이미 광고를 보여 준 상태에서") {
            When("한 번 더 보여 줬다고 기록하면") {
                Then("오늘 횟수를 이어서 세야 한다") {
                    val repository = FakeInterstitialAdRepository(
                        InterstitialAdRecord(lastShownEpochDay = today, shownCountOfDay = 2),
                    )

                    RecordInterstitialAdShownUseCase(repository, clock)()

                    repository.record.shownCountOfDay shouldBe 3
                }
            }
        }

        Given("어제 광고를 보여 준 상태에서") {
            When("오늘 처음 보여 줬다고 기록하면") {
                Then("오늘 횟수를 1부터 다시 세야 한다") {
                    val repository = FakeInterstitialAdRepository(
                        InterstitialAdRecord(lastShownEpochDay = today - 1, shownCountOfDay = 3),
                    )

                    RecordInterstitialAdShownUseCase(repository, clock)()

                    repository.record.lastShownEpochDay shouldBe today
                    repository.record.shownCountOfDay shouldBe 1
                }
            }
        }
    })
