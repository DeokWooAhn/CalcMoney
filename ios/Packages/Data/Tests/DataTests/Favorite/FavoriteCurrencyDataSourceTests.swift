import Foundation
import Testing
@testable import Data

@Suite("FavoriteCurrencyDataSource")
struct FavoriteCurrencyDataSourceTests {
    private func makeDataSource() -> FavoriteCurrencyDataSource {
        let suiteName = "test-\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)

        return FavoriteCurrencyDataSource(userDefaults: userDefaults)
    }

    @Test("즐겨찾기를 추가하면 isFavorite이 true가 된다")
    func 즐겨찾기_추가() async {
        let dataSource = makeDataSource()

        await dataSource.addFavorite("USD")

        #expect(await dataSource.isFavorite("USD"))
        #expect(await !dataSource.isFavorite("JPY"))
    }

    @Test("같은 통화를 두 번 추가해도 한 번만 저장한다")
    func 중복_추가_방지() async {
        let dataSource = makeDataSource()

        await dataSource.addFavorite("USD")
        await dataSource.addFavorite("USD")
        await dataSource.addFavorite("JPY")

        var iterator = dataSource.favoriteCodes().makeAsyncIterator()
        let codes = await iterator.next()

        #expect(codes == ["USD", "JPY"])
    }

    @Test("즐겨찾기를 제거하면 목록에서 사라진다")
    func 즐겨찾기_제거() async {
        let dataSource = makeDataSource()
        await dataSource.addFavorite("USD")
        await dataSource.addFavorite("JPY")

        await dataSource.removeFavorite("USD")

        var iterator = dataSource.favoriteCodes().makeAsyncIterator()
        let codes = await iterator.next()

        #expect(codes == ["JPY"])
    }
}
