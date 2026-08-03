public protocol CalculatorHistoryRepository: Sendable {
    func histories() -> AsyncStream<[CalculatorHistory]>

    func addHistory(_ history: CalculatorHistory) async throws

    func clearHistories() async throws
}
