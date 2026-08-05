import Domain

public struct CalculatorHistoryRepositoryImpl: CalculatorHistoryRepository {
    private let dataSource: CalculatorHistoryDataSource

    public init(dataSource: CalculatorHistoryDataSource) {
        self.dataSource = dataSource
    }

    public func histories() -> AsyncStream<[CalculatorHistory]> {
        dataSource.histories()
    }

    public func addHistory(_ history: CalculatorHistory) async throws {
        await dataSource.addHistory(history)
    }

    public func clearHistories() async throws {
        await dataSource.clearHistories()
    }
}
