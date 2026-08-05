import Domain
import Foundation
import Testing
@testable import Data

@Suite("ThemePreferenceDataSource")
struct ThemePreferenceDataSourceTests {
    private func makeDataSource() -> ThemePreferenceDataSource {
        let suiteName = "test-\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)

        return ThemePreferenceDataSource(userDefaults: userDefaults)
    }

    @Test("저장된 값이 없으면 시스템 테마를 방출한다")
    func 기본값은_시스템_테마() async {
        let dataSource = makeDataSource()

        var iterator = dataSource.themeMode().makeAsyncIterator()
        let mode = await iterator.next()

        #expect(mode == .system)
    }

    @Test("저장한 테마 모드를 스트림 초기값으로 방출한다")
    func 저장한_테마_모드_방출() async {
        let dataSource = makeDataSource()

        await dataSource.saveThemeMode(.dark)

        var iterator = dataSource.themeMode().makeAsyncIterator()
        let mode = await iterator.next()

        #expect(mode == .dark)
    }
}
