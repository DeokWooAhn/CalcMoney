import Domain
import Foundation
import Testing
@testable import Data

/// 캐시가 만료된 뒤 원격 조회를 5분에 한 번만 다시 시도하는지 본다.
/// 결정 기록: `docs/decisions/exchange-rate-remote-retry-cooldown.md`
@Suite("ExchangeRateRepositoryImpl 원격 재시도 쿨다운")
struct ExchangeRateRepositoryRetryCooldownTests {
    private static let usd = ExchangeRateData(
        code: "USD",
        currencyUnit: "USD",
        currencyName: "미국 달러",
        baseRate: 1441.1,
        fetchedAt: 1000,
        rateDate: "2026-08-04",
    )
    /// `usd`의 fetchedAt(1000)에서 13시간이 지나 캐시가 만료된 시각
    private static let expiredMillis = 1000 + 13 * 60 * 60 * 1000
    private static let remoteRetryCooldownMillis = 5 * 60 * 1000

    /// remoteRates가 nil이면 원격 조회가 늘 실패한다.
    private func makeRepository(
        remoteRates: [ExchangeRateData]? = nil,
        cachedRates: [ExchangeRateData] = [Self.usd],
        clock: TestClock = TestClock(millis: Self.expiredMillis),
    ) async throws -> (ExchangeRateRepositoryImpl, RemoteDataSourceStub) {
        let remote = RemoteDataSourceStub(result: remoteRates)
        let local = try ExchangeRateLocalDataSource.make(inMemory: true)

        if !cachedRates.isEmpty {
            try await local.replaceRates(cachedRates)
        }

        let repository = ExchangeRateRepositoryImpl(
            remoteDataSource: remote,
            localDataSource: local,
            now: { clock.date },
        )

        return (repository, remote)
    }

    @Test("실패 직후 5분 안의 연속 조회는 원격을 다시 부르지 않고 만료된 캐시를 쓴다")
    func 실패_직후_5분_안의_연속_조회는_원격을_다시_부르지_않는다() async throws {
        let clock = TestClock(millis: Self.expiredMillis)
        let (repository, remote) = try await makeRepository(clock: clock)

        #expect(try await repository.exchangeRate(from: "USD", to: "KRW") == 1441.1)
        clock.advance(by: Self.remoteRetryCooldownMillis - 1)
        #expect(try await repository.exchangeRate(from: "USD", to: "KRW") == 1441.1)
        #expect(try await repository.supportedCurrencies().map(\.code) == ["KRW", "USD"])

        #expect(await remote.fetchCallCount == 1)
    }

    @Test("실패 뒤 5분이 지나면 원격을 다시 부른다")
    func 실패_뒤_5분이_지나면_원격을_다시_부른다() async throws {
        let clock = TestClock(millis: Self.expiredMillis)
        let (repository, remote) = try await makeRepository(clock: clock)

        _ = try await repository.exchangeRate(from: "USD", to: "KRW")
        clock.advance(by: Self.remoteRetryCooldownMillis)
        _ = try await repository.exchangeRate(from: "USD", to: "KRW")

        #expect(await remote.fetchCallCount == 2)
    }

    @Test("원격 조회가 성공했어도 받은 문서가 만료 상태면 5분 안에는 원격을 다시 부르지 않는다")
    func 성공했어도_만료된_문서면_5분_안에는_원격을_다시_부르지_않는다() async throws {
        let (repository, remote) = try await makeRepository(remoteRates: [Self.usd])

        _ = try await repository.exchangeRate(from: "USD", to: "KRW")
        _ = try await repository.exchangeRate(from: "USD", to: "KRW")

        #expect(await remote.fetchCallCount == 1)
    }

    @Test("실패 직후에도 수동 새로고침은 원격을 부른다")
    func 실패_직후에도_수동_새로고침은_원격을_부른다() async throws {
        let (repository, remote) = try await makeRepository()

        _ = try await repository.exchangeRate(from: "USD", to: "KRW")
        await #expect(throws: ExchangeRateError.self) {
            try await repository.refreshExchangeRates()
        }

        #expect(await remote.fetchCallCount == 2)
    }

    @Test("수동 새로고침이 실패한 직후의 조회는 원격을 다시 부르지 않는다")
    func 수동_새로고침_실패_직후의_조회는_원격을_다시_부르지_않는다() async throws {
        let (repository, remote) = try await makeRepository()

        await #expect(throws: ExchangeRateError.self) {
            try await repository.refreshExchangeRates()
        }
        #expect(try await repository.exchangeRate(from: "USD", to: "KRW") == 1441.1)

        #expect(await remote.fetchCallCount == 1)
    }

    @Test("기기 시각이 마지막 시도보다 이전으로 돌아가면 원격을 다시 부른다")
    func 기기_시각이_이전으로_돌아가면_원격을_다시_부른다() async throws {
        let clock = TestClock(millis: Self.expiredMillis)
        let (repository, remote) = try await makeRepository(clock: clock)

        _ = try await repository.exchangeRate(from: "USD", to: "KRW")
        clock.advance(by: -1)
        _ = try await repository.exchangeRate(from: "USD", to: "KRW")

        #expect(await remote.fetchCallCount == 2)
    }

    @Test("캐시가 없으면 실패가 이어져도 호출마다 원격을 시도한다")
    func 캐시가_없으면_호출마다_원격을_시도한다() async throws {
        let (repository, remote) = try await makeRepository(cachedRates: [])

        await #expect(throws: ExchangeRateError.self) {
            _ = try await repository.exchangeRate(from: "USD", to: "KRW")
        }
        await #expect(throws: ExchangeRateError.self) {
            _ = try await repository.supportedCurrencies()
        }

        #expect(await remote.fetchCallCount == 2)
    }
}
