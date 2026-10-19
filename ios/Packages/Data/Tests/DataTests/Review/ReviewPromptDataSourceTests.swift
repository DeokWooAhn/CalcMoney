import Domain
import Foundation
import Testing
@testable import Data

@Suite("ReviewPromptDataSource")
struct ReviewPromptDataSourceTests {
    private func makeDataSource() -> ReviewPromptDataSource {
        let suiteName = "test-\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)

        return ReviewPromptDataSource(userDefaults: userDefaults)
    }

    private func currentRecord(_ dataSource: ReviewPromptDataSource) async -> ReviewPromptRecord {
        await dataSource.updateRecord { $0 }
    }

    @Test("저장된 기록이 없으면 빈 기록을 반환한다")
    func 저장된_기록이_없으면_빈_기록() async {
        let dataSource = makeDataSource()

        #expect(await currentRecord(dataSource) == ReviewPromptRecord())
    }

    @Test("모든 값을 채운 기록을 저장하면 그대로 다시 읽힌다")
    func 저장한_기록을_그대로_읽는다() async {
        let dataSource = makeDataSource()
        let record = ReviewPromptRecord(
            firstOpenedAt: Date(timeIntervalSince1970: 1000),
            lastOpenedAt: Date(timeIntervalSince1970: 2000),
            openedDayCount: 3,
            calculationSuccessCount: 7,
            lastRequestedAt: Date(timeIntervalSince1970: 3000),
        )

        await dataSource.updateRecord { $0 = record }

        #expect(await currentRecord(dataSource) == record)
    }

    @Test("값을 nil로 바꿔 저장하면 지워진다")
    func nil로_바꾸면_지워진다() async {
        let dataSource = makeDataSource()
        await dataSource.updateRecord {
            $0 = ReviewPromptRecord(
                firstOpenedAt: Date(timeIntervalSince1970: 1000),
                lastRequestedAt: Date(timeIntervalSince1970: 3000),
            )
        }

        await dataSource.updateRecord { $0.lastRequestedAt = nil }

        #expect(await currentRecord(dataSource) == ReviewPromptRecord(firstOpenedAt: Date(timeIntervalSince1970: 1000)))
    }

    @Test("transform이 돌려준 값을 그대로 반환한다")
    func transform_결과를_반환한다() async {
        let dataSource = makeDataSource()

        let count = await dataSource.updateRecord { record -> Int in
            record.calculationSuccessCount += 1
            return record.calculationSuccessCount
        }

        #expect(count == 1)
        #expect(await currentRecord(dataSource).calculationSuccessCount == 1)
    }
}
