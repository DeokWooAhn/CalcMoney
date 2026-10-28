import Domain
import Foundation
import Testing
@testable import Data

@Suite("InterstitialAdDataSource")
struct InterstitialAdDataSourceTests {
    private func makeDataSource() -> InterstitialAdDataSource {
        let suiteName = "test-\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)

        return InterstitialAdDataSource(userDefaults: userDefaults)
    }

    @Test("저장된 기록이 없으면 빈 기록을 반환한다")
    func 저장된_기록이_없으면_빈_기록() async {
        let dataSource = makeDataSource()

        #expect(await dataSource.record() == InterstitialAdRecord())
    }

    @Test("모든 값을 채운 기록을 저장하면 그대로 다시 읽힌다")
    func 저장한_기록을_그대로_읽는다() async {
        let dataSource = makeDataSource()
        let record = InterstitialAdRecord(
            firstCalculatedAt: Date(timeIntervalSince1970: 1000),
            calculationCountSinceLastAd: 4,
            lastShownAt: Date(timeIntervalSince1970: 2000),
            shownCountOfDay: 2,
        )

        await dataSource.updateRecord { $0 = record }

        #expect(await dataSource.record() == record)
    }

    @Test("보여 준 시각을 nil로 바꿔 저장하면 지워진다")
    func 보여_준_시각을_지운다() async {
        let dataSource = makeDataSource()
        await dataSource.updateRecord {
            $0 = InterstitialAdRecord(
                firstCalculatedAt: Date(timeIntervalSince1970: 1),
                lastShownAt: Date(timeIntervalSince1970: 2),
            )
        }

        await dataSource.updateRecord { $0.lastShownAt = nil }

        #expect(await dataSource.record() == InterstitialAdRecord(firstCalculatedAt: Date(timeIntervalSince1970: 1)))
    }
}
