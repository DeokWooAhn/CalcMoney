package com.ahn.domain.review.usecase

import com.ahn.domain.review.FakeReviewPromptRepository
import com.ahn.domain.review.model.ReviewPromptRecord
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class RecordAppOpenUseCaseTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val seoul = ZoneId.of("Asia/Seoul")

        fun clockAt(dateTime: String, zone: ZoneId = seoul): Clock {
            return Clock.fixed(Instant.parse(dateTime), zone)
        }

        Given("앱을 처음 여는 상태에서") {
            When("앱 열기를 기록하면") {
                Then("첫 실행 시각과 오늘 날짜를 남기고 사용 일수를 1로 만들어야 한다") {
                    val clock = clockAt("2026-10-20T01:00:00Z")
                    val repository = FakeReviewPromptRepository()

                    RecordAppOpenUseCase(repository, clock)()

                    repository.record shouldBe ReviewPromptRecord(
                        firstOpenedAt = clock.millis(),
                        lastOpenedEpochDay = LocalDate.of(2026, 10, 20).toEpochDay(),
                        openedDayCount = 1,
                    )
                }
            }
        }

        Given("오늘 이미 앱을 연 기록이 있는 상태에서") {
            When("같은 날 다시 앱 열기를 기록하면") {
                Then("기록이 바뀌지 않아야 한다") {
                    val today = LocalDate.of(2026, 10, 20).toEpochDay()
                    val existing = ReviewPromptRecord(
                        firstOpenedAt = 1L,
                        lastOpenedEpochDay = today,
                        openedDayCount = 2,
                    )
                    val repository = FakeReviewPromptRepository(existing)

                    RecordAppOpenUseCase(repository, clockAt("2026-10-20T14:00:00Z"))()

                    repository.record shouldBe existing
                }
            }
        }

        Given("어제 앱을 연 기록이 있는 상태에서") {
            When("오늘 앱 열기를 기록하면") {
                Then("첫 실행 시각은 그대로 두고 사용 일수를 하나 늘려야 한다") {
                    val existing = ReviewPromptRecord(
                        firstOpenedAt = 1L,
                        lastOpenedEpochDay = LocalDate.of(2026, 10, 19).toEpochDay(),
                        openedDayCount = 2,
                    )
                    val repository = FakeReviewPromptRepository(existing)

                    RecordAppOpenUseCase(repository, clockAt("2026-10-20T01:00:00Z"))()

                    repository.record shouldBe existing.copy(
                        lastOpenedEpochDay = LocalDate.of(2026, 10, 20).toEpochDay(),
                        openedDayCount = 3,
                    )
                }
            }
        }

        Given("UTC로는 같은 날이지만 기기 시간대로는 날짜가 바뀐 상태에서") {
            When("앱 열기를 기록하면") {
                Then("기기 시간대 기준으로 다른 날로 세야 한다") {
                    // 2026-10-19T14:30Z는 서울 기준 10월 19일 23:30, 2026-10-19T15:30Z는 10월 20일 00:30이다.
                    val repository = FakeReviewPromptRepository()

                    RecordAppOpenUseCase(repository, clockAt("2026-10-19T14:30:00Z"))()
                    RecordAppOpenUseCase(repository, clockAt("2026-10-19T15:30:00Z"))()

                    repository.record.openedDayCount shouldBe 2
                    repository.record.lastOpenedEpochDay shouldBe LocalDate.of(2026, 10, 20).toEpochDay()
                }
            }
        }

        Given("UTC 시간대의 기기에서") {
            When("같은 두 시각에 앱 열기를 기록하면") {
                Then("같은 날로 세야 한다") {
                    val repository = FakeReviewPromptRepository()

                    RecordAppOpenUseCase(repository, clockAt("2026-10-19T14:30:00Z", ZoneOffset.UTC))()
                    RecordAppOpenUseCase(repository, clockAt("2026-10-19T15:30:00Z", ZoneOffset.UTC))()

                    repository.record.openedDayCount shouldBe 1
                }
            }
        }
    })
