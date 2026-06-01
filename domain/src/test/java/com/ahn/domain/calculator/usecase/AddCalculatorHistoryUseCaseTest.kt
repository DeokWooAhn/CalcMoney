package com.ahn.domain.calculator.usecase

import com.ahn.domain.calculator.model.CalculatorHistory
import com.ahn.domain.calculator.repository.CalculatorHistoryRepository
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class AddCalculatorHistoryUseCaseTest :
    DescribeSpec({

        isolationMode = IsolationMode.InstancePerRoot

        val repository = mockk<CalculatorHistoryRepository>()
        val useCase = AddCalculatorHistoryUseCase(repository)

        beforeEach {
            clearAllMocks()
        }

        describe("계산 기록 추가") {
            it("repository에 계산 기록 추가를 요청한다") {
                val history = CalculatorHistory(
                    expression = "1+1",
                    result = "2",
                )

                coEvery { repository.addHistory(history) } returns Unit

                useCase(history)

                coVerify(exactly = 1) {
                    repository.addHistory(history)
                }
            }
        }
    })
