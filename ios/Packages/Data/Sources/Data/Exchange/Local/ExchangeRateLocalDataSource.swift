import Foundation
import SwiftData

/// 환율 캐시를 SwiftData에 저장하는 로컬 데이터소스 (Android `ExchangeRateLocalDataSource` + DAO 대응)
@ModelActor
public actor ExchangeRateLocalDataSource {
    /// 기본 디스크 저장소(또는 테스트용 메모리 저장소)를 사용하는 데이터소스를 만든다.
    public static func make(inMemory: Bool = false) throws -> ExchangeRateLocalDataSource {
        let configuration = ModelConfiguration(isStoredInMemoryOnly: inMemory)
        let container = try ModelContainer(for: ExchangeRateRecord.self, configurations: configuration)

        return ExchangeRateLocalDataSource(modelContainer: container)
    }

    public func cachedRates() throws -> [ExchangeRateData] {
        try fetchAllRecords().map(\.asData)
    }

    public func latestFetchedAt() throws -> Int {
        try fetchAllRecords().map(\.fetchedAt).max() ?? 0
    }

    public func latestRateDate() throws -> String {
        try fetchAllRecords().map(\.rateDate).max() ?? ""
    }

    /// 캐시 전체를 새 환율 목록으로 교체한다 (Android DAO `replaceAll` 대응).
    public func replaceRates(_ rates: [ExchangeRateData]) throws {
        try modelContext.delete(model: ExchangeRateRecord.self)

        for (index, rate) in rates.enumerated() {
            modelContext.insert(ExchangeRateRecord(rate, sortOrder: index))
        }

        try modelContext.save()
    }

    private func fetchAllRecords() throws -> [ExchangeRateRecord] {
        try modelContext.fetch(
            FetchDescriptor<ExchangeRateRecord>(sortBy: [SortDescriptor(\.sortOrder)]),
        )
    }
}
