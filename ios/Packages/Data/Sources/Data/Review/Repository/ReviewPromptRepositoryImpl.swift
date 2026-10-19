import Domain

public struct ReviewPromptRepositoryImpl: ReviewPromptRepository {
    private let dataSource: ReviewPromptDataSource

    public init(dataSource: ReviewPromptDataSource) {
        self.dataSource = dataSource
    }

    public func updateRecord<Result: Sendable>(
        _ transform: @Sendable (inout ReviewPromptRecord) -> Result,
    ) async -> Result {
        await dataSource.updateRecord(transform)
    }
}
