import Testing
@testable import Domain

@Suite("AddCalculatorHistoryUseCase — 계산 기록 추가")
struct AddCalculatorHistoryUseCaseTests {
    @Test("repository에 계산 기록 추가를 요청한다")
    func repository에_계산_기록_추가를_요청한다() async throws {
        let repository = CalculatorHistoryRepositoryStub()
        let useCase = AddCalculatorHistoryUseCase(repository: repository)
        let history = CalculatorHistory(expression: "1+1", result: "2")

        try await useCase(history)

        #expect(repository.addedHistories == [history])
    }
}

/// 계산 기록 저장 호출을 기록하는 테스트 스텁 (Android MockK 대응)
private final class CalculatorHistoryRepositoryStub: CalculatorHistoryRepository, @unchecked Sendable {
    private(set) var addedHistories: [CalculatorHistory] = []

    func histories() -> AsyncStream<[CalculatorHistory]> {
        AsyncStream { $0.finish() }
    }

    func addHistory(_ history: CalculatorHistory) async throws {
        addedHistories.append(history)
    }

    func clearHistories() async throws {}
}
