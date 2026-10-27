package com.ahn.data.ads.local.datasource

import com.ahn.data.common.datastore.createTestPreferenceDataStore
import com.ahn.domain.ads.model.InterstitialAdRecord
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class InterstitialAdDataSourceTest :
    BehaviorSpec({

        isolationMode = IsolationMode.InstancePerRoot

        Given("저장된 기록이 없는 상태에서") {
            When("기록을 읽으면") {
                Then("빈 기록을 반환해야 한다") {
                    runTest {
                        val dataSource = InterstitialAdDataSource(createTestPreferenceDataStore("ad-empty"))

                        dataSource.getRecord() shouldBe InterstitialAdRecord()
                    }
                }
            }
        }

        Given("모든 값을 채운 기록을 저장한 상태에서") {
            When("다시 읽으면") {
                Then("저장한 값이 그대로 돌아와야 한다") {
                    runTest {
                        val dataSource = InterstitialAdDataSource(createTestPreferenceDataStore("ad-full"))
                        val record = InterstitialAdRecord(
                            firstCalculatedAt = 1_000L,
                            calculationCountSinceLastAd = 4,
                            lastShownAt = 2_000L,
                            lastShownEpochDay = 20_000L,
                            shownCountOfDay = 2,
                        )

                        dataSource.updateRecord { record } shouldBe record

                        dataSource.getRecord() shouldBe record
                    }
                }
            }
        }

        Given("보여 준 시각이 있는 기록에서") {
            When("보여 준 시각과 날짜를 null로 바꿔 저장하면") {
                Then("두 값이 지워져야 한다") {
                    runTest {
                        val dataSource = InterstitialAdDataSource(createTestPreferenceDataStore("ad-remove"))
                        dataSource.updateRecord {
                            InterstitialAdRecord(firstCalculatedAt = 1L, lastShownAt = 2L, lastShownEpochDay = 3L)
                        }

                        dataSource.updateRecord { it.copy(lastShownAt = null, lastShownEpochDay = null) }

                        dataSource.getRecord() shouldBe InterstitialAdRecord(firstCalculatedAt = 1L)
                    }
                }
            }
        }
    })
