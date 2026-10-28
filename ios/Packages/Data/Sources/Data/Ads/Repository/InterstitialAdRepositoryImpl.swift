import Domain

public struct InterstitialAdRepositoryImpl: InterstitialAdRepository {
    private let dataSource: InterstitialAdDataSource

    public init(dataSource: InterstitialAdDataSource) {
        self.dataSource = dataSource
    }

    public func record() async -> InterstitialAdRecord {
        await dataSource.record()
    }

    public func updateRecord(_ transform: @Sendable (inout InterstitialAdRecord) -> Void) async {
        await dataSource.updateRecord(transform)
    }
}
