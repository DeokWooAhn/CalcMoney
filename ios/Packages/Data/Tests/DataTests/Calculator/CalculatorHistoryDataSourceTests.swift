import Domain
import Foundation
import Testing
@testable import Data

@Suite("CalculatorHistoryDataSource")
struct CalculatorHistoryDataSourceTests {
    private func makeDataSource() -> CalculatorHistoryDataSource {
        let suiteName = "test-\(UUID().uuidString)"
        let userDefaults = UserDefaults(suiteName: suiteName)!
        userDefaults.removePersistentDomain(forName: suiteName)

        return CalculatorHistoryDataSource(userDefaults: userDefaults)
    }

    @Test("추가한 기록을 스트림 초기값으로 방출한다")
    func 추가한_기록을_스트림_초기값으로_방출한다() async throws {
        let dataSource = makeDataSource()
        let history = CalculatorHistory(expression: "1+1", result: "2")

        await dataSource.addHistory(history)

        var iterator = dataSource.histories().makeAsyncIterator()
        let first = await iterator.next()

        #expect(first == [history])
    }

    @Test("기록은 최대 20개까지만 보관한다")
    func 기록은_최대_20개까지만_보관한다() async throws {
        let dataSource = makeDataSource()

        for index in 1...25 {
            await dataSource.addHistory(CalculatorHistory(expression: "\(index)+0", result: "\(index)"))
        }

        var iterator = dataSource.histories().makeAsyncIterator()
        let histories = await iterator.next() ?? []

        #expect(histories.count == 20)
        #expect(histories.first?.result == "6")
        #expect(histories.last?.result == "25")
    }

    @Test("기록 전체 삭제 후 빈 목록을 방출한다")
    func 기록_전체_삭제_후_빈_목록을_방출한다() async throws {
        let dataSource = makeDataSource()
        await dataSource.addHistory(CalculatorHistory(expression: "1+1", result: "2"))

        await dataSource.clearHistories()

        var iterator = dataSource.histories().makeAsyncIterator()
        let histories = await iterator.next()

        #expect(histories == [])
    }
}
