package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class SaveCalculatorHistoriesUseCaseTest : DescribeSpec({

    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<CalculatorHistoryRepository>()
    val useCase = SaveCalculatorHistoriesUseCase(repository)

    beforeEach {
        clearAllMocks()
    }

    describe("계산 기록 저장") {
        it("계산 기록을 repository에 저장한다") {
            val histories = listOf(
                CalculatorHistory(expression = "1+1", result = "2"),
                CalculatorHistory(expression = "2+2", result = "4"),
            )

            coEvery { repository.saveHistories(any()) } returns Unit

            useCase(histories)

            coVerify(exactly = 1) {
                repository.saveHistories(histories)
            }
        }

        it("계산 기록이 20개를 초과하면 최근 20개만 저장한다") {
            val histories = (1..25).map { index ->
                CalculatorHistory(
                    expression = "$index+$index",
                    result = "${index + index}",
                )
            }

            coEvery { repository.saveHistories(any()) } returns Unit

            useCase(histories)

            coVerify(exactly = 1) {
                repository.saveHistories(
                    withArg { saveHistories ->
                        saveHistories.map { it.expression } shouldContainExactly
                                (6..25).map { "$it+$it" }
                    }
                )
            }
        }
    }
})