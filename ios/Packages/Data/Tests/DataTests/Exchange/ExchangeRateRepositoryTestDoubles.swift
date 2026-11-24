import Domain
import Foundation
import os
@testable import Data

/// 원격 호출 횟수를 기록하는 스텁. result가 nil이면 오류를 던진다.
actor RemoteDataSourceStub: ExchangeRateRemoteDataSource {
    private let result: [ExchangeRateData]?
    private(set) var fetchCallCount = 0

    init(result: [ExchangeRateData]?) {
        self.result = result
    }

    func fetchExchangeRates() async throws -> [ExchangeRateData] {
        fetchCallCount += 1

        guard let result else { throw ExchangeRateError.networkUnavailable() }

        return result
    }
}

/// 테스트가 시각을 앞뒤로 옮길 수 있는 시계
final class TestClock: Sendable {
    private let state: OSAllocatedUnfairLock<Int>

    init(millis: Int) {
        state = OSAllocatedUnfairLock(initialState: millis)
    }

    var date: Date {
        Date(timeIntervalSince1970: Double(state.withLock { $0 }) / 1000)
    }

    func advance(by millis: Int) {
        state.withLock { $0 += millis }
    }
}
