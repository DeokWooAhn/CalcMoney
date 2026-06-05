package com.ahn.data.calculator.local.datasource

import com.ahn.data.common.datastore.createTestPreferenceDataStore
import com.ahn.domain.calculator.model.CalculatorHistory
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class CalculatorHistoryDataSourceTest :
    DescribeSpec({
        describe("계산 기록 DataStore") {
            it("저장된 계산 기록이 없으면 빈 목록을 반환한다") {
                runTest {
                    val dataSource = CalculatorHistoryDataSource(
                        dataStore = createTestPreferenceDataStore("history-empty"),
                    )

                    dataSource.getHistories().first() shouldBe emptyList()
                }
            }

            it("계산 기록을 저장하고 반환한다") {
                runTest {
                    val dataSource = CalculatorHistoryDataSource(
                        dataStore = createTestPreferenceDataStore("history-add"),
                    )
                    val histories = listOf(
                        CalculatorHistory(expression = "1+1", result = "2"),
                        CalculatorHistory(expression = "2×3", result = "6"),
                    )

                    histories.forEach { history ->
                        dataSource.addHistory(history)
                    }

                    dataSource.getHistories().first() shouldContainExactly histories
                }
            }

            it("계산 기록은 최근 20개만 유지한다") {
                runTest {
                    val dataSource = CalculatorHistoryDataSource(
                        dataStore = createTestPreferenceDataStore("history-limit"),
                    )

                    repeat(25) { index ->
                        dataSource.addHistory(
                            CalculatorHistory(
                                expression = "$index+1",
                                result = "${index + 1}",
                            ),
                        )
                    }

                    val histories = dataSource.getHistories().first()
                    histories.size shouldBe 20
                    histories.first() shouldBe CalculatorHistory(expression = "5+1", result = "6")
                    histories.last() shouldBe CalculatorHistory(expression = "24+1", result = "25")
                }
            }

            it("계산 기록을 삭제한다") {
                runTest {
                    val dataSource = CalculatorHistoryDataSource(
                        dataStore = createTestPreferenceDataStore("history-clear"),
                    )

                    dataSource.addHistory(CalculatorHistory(expression = "1+1", result = "2"))
                    dataSource.clearHistories()

                    dataSource.getHistories().first() shouldBe emptyList()
                }
            }
        }
    })
