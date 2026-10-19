package com.ahn.data.review.local.datasource

import com.ahn.data.common.datastore.createTestPreferenceDataStore
import com.ahn.domain.review.model.ReviewPromptRecord
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class ReviewPromptDataSourceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        Given("저장된 기록이 없는 상태에서") {
            When("기록을 그대로 두고 갱신하면") {
                Then("빈 기록을 반환해야 한다") {
                    runTest {
                        val dataSource = ReviewPromptDataSource(createTestPreferenceDataStore("review-empty"))

                        dataSource.updateRecord { it } shouldBe ReviewPromptRecord()
                    }
                }
            }
        }

        Given("모든 값을 채운 기록을 저장한 상태에서") {
            When("다시 읽으면") {
                Then("저장한 값이 그대로 돌아와야 한다") {
                    runTest {
                        val dataSource = ReviewPromptDataSource(createTestPreferenceDataStore("review-full"))
                        val record = ReviewPromptRecord(
                            firstOpenedAt = 1_000L,
                            lastOpenedEpochDay = 20_000L,
                            openedDayCount = 3,
                            calculationSuccessCount = 7,
                            lastRequestedAt = 2_000L,
                        )

                        dataSource.updateRecord { record }

                        dataSource.updateRecord { it } shouldBe record
                    }
                }
            }
        }

        Given("요청 시각이 있는 기록에서") {
            When("요청 시각을 null로 바꿔 저장하면") {
                Then("요청 시각이 지워져야 한다") {
                    runTest {
                        val dataSource = ReviewPromptDataSource(createTestPreferenceDataStore("review-remove"))
                        dataSource.updateRecord { ReviewPromptRecord(firstOpenedAt = 1L, lastRequestedAt = 2L) }

                        dataSource.updateRecord { it.copy(lastRequestedAt = null) }

                        dataSource.updateRecord { it } shouldBe ReviewPromptRecord(firstOpenedAt = 1L)
                    }
                }
            }
        }

        Given("기존 기록이 있는 상태에서") {
            When("기존 값을 바탕으로 갱신하면") {
                Then("이전 값에 이어서 저장해야 한다") {
                    runTest {
                        val dataSource = ReviewPromptDataSource(createTestPreferenceDataStore("review-increment"))

                        repeat(3) {
                            dataSource.updateRecord {
                                it.copy(calculationSuccessCount = it.calculationSuccessCount + 1)
                            }
                        }

                        dataSource.updateRecord { it }.calculationSuccessCount shouldBe 3
                    }
                }
            }
        }
    })
