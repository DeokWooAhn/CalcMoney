import Domain
import Foundation
import Testing
@testable import Data

@Suite("ExchangeRateRepositoryImpl")
struct ExchangeRateRepositoryImplTests {
    private static let usd = ExchangeRateData(
        code: "USD",
        currencyUnit: "USD",
        currencyName: "미국 달러",
        baseRate: 1441.1,
        fetchedAt: 1000,
        rateDate: "2026-08-04",
    )
    private static let jpy = ExchangeRateData(
        code: "JPY",
        currencyUnit: "JPY",
        currencyName: "일본 엔",
        baseRate: 9.65,
        fetchedAt: 1000,
        rateDate: "2026-08-04",
    )

    private func makeRepository(
        remoteRates: [ExchangeRateData]? = nil,
        cachedRates: [ExchangeRateData] = [],
        nowMillis: Int = 0,
    ) async throws -> (ExchangeRateRepositoryImpl, RemoteDataSourceStub) {
        let remote = RemoteDataSourceStub(result: remoteRates)
        let local = try ExchangeRateLocalDataSource.make(inMemory: true)

        if !cachedRates.isEmpty {
            try await local.replaceRates(cachedRates)
        }

        let repository = ExchangeRateRepositoryImpl(
            remoteDataSource: remote,
            localDataSource: local,
            now: { Date(timeIntervalSince1970: Double(nowMillis) / 1000) },
        )

        return (repository, remote)
    }

    @Test("캐시가 유효하면 원격을 호출하지 않는다")
    func 캐시가_유효하면_원격을_호출하지_않는다() async throws {
        let (repository, remote) = try await makeRepository(
            remoteRates: [Self.usd],
            cachedRates: [Self.usd, Self.jpy],
            nowMillis: 1000 + 11 * 60 * 60 * 1000,
        )

        let rate = try await repository.exchangeRate(from: "USD", to: "KRW")

        #expect(rate == 1441.1)
        #expect(await remote.fetchCallCount == 0)
    }

    @Test("캐시가 만료되면 원격에서 받아 캐시를 갱신한다")
    func 캐시가_만료되면_원격에서_받아_캐시를_갱신한다() async throws {
        let fresh = ExchangeRateData(
            code: "USD",
            currencyUnit: "USD",
            currencyName: "미국 달러",
            baseRate: 1500.0,
            fetchedAt: 999_999,
            rateDate: "2026-08-05",
        )
        let (repository, remote) = try await makeRepository(
            remoteRates: [fresh],
            cachedRates: [Self.usd],
            nowMillis: 1000 + 13 * 60 * 60 * 1000,
        )

        let rate = try await repository.exchangeRate(from: "USD", to: "KRW")

        #expect(rate == 1500.0)
        #expect(await remote.fetchCallCount == 1)
        #expect(try await repository.latestRateDate() == "2026-08-05")
    }

    @Test("원격 조회에 실패해도 캐시가 있으면 캐시를 반환한다")
    func 원격_실패_시_캐시를_반환한다() async throws {
        let (repository, _) = try await makeRepository(
            remoteRates: nil,
            cachedRates: [Self.usd],
            nowMillis: 1000 + 13 * 60 * 60 * 1000,
        )

        let rate = try await repository.exchangeRate(from: "USD", to: "KRW")

        #expect(rate == 1441.1)
    }

    @Test("원격 조회에 실패하고 캐시도 없으면 오류를 던진다")
    func 원격_실패_캐시_없음은_오류() async throws {
        let (repository, _) = try await makeRepository(remoteRates: nil)

        await #expect(throws: ExchangeRateError.self) {
            _ = try await repository.exchangeRate(from: "USD", to: "KRW")
        }
    }

    @Test("같은 통화면 1.0을 반환한다")
    func 같은_통화면_1을_반환한다() async throws {
        let (repository, remote) = try await makeRepository()

        let rate = try await repository.exchangeRate(from: "USD", to: "USD")

        #expect(rate == 1.0)
        #expect(await remote.fetchCallCount == 0)
    }

    @Test("두 통화 간 환율은 기준 환율의 비율로 계산한다")
    func 두_통화_간_환율은_기준_환율의_비율() async throws {
        let (repository, _) = try await makeRepository(
            cachedRates: [Self.usd, Self.jpy],
            nowMillis: 1000,
        )

        let rate = try await repository.exchangeRate(from: "USD", to: "JPY")

        #expect(abs(rate - 1441.1 / 9.65) < 0.0001)
    }

    @Test("목록에 없는 통화는 rateNotFound 오류를 던진다")
    func 없는_통화는_rateNotFound() async throws {
        let (repository, _) = try await makeRepository(
            cachedRates: [Self.usd],
            nowMillis: 1000,
        )

        do {
            _ = try await repository.exchangeRate(from: "USD", to: "VND")
            Issue.record("오류가 발생해야 한다")
        } catch let error as ExchangeRateError {
            guard case let .rateNotFound(code, _) = error, code == "VND" else {
                Issue.record("rateNotFound(VND)가 아닌 오류: \(error)")
                return
            }
        }
    }

    @Test("지원 통화 목록은 KRW를 항상 포함하고 중복을 제거한다")
    func 지원_통화_목록은_KRW_포함_중복_제거() async throws {
        let krwDuplicate = ExchangeRateData(
            code: "KRW",
            currencyUnit: "KRW",
            currencyName: "대한민국 원",
            baseRate: 1.0,
            fetchedAt: 1000,
            rateDate: "2026-08-04",
        )
        let (repository, _) = try await makeRepository(
            cachedRates: [Self.usd, krwDuplicate, Self.jpy],
            nowMillis: 1000,
        )

        let currencies = try await repository.supportedCurrencies()

        #expect(currencies.map(\.code) == ["KRW", "USD", "JPY"])
        #expect(currencies[0].name == "한국 원")
        #expect(currencies[0].flagEmoji == "🇰🇷")
        #expect(currencies[1].flagEmoji == "🇺🇸")
    }

    @Test("refreshExchangeRates는 TTL과 무관하게 원격을 호출한다")
    func refresh는_TTL과_무관하게_원격_호출() async throws {
        let (repository, remote) = try await makeRepository(
            remoteRates: [Self.usd],
            cachedRates: [Self.jpy],
            nowMillis: 1000,
        )

        try await repository.refreshExchangeRates()

        #expect(await remote.fetchCallCount == 1)
        #expect(try await repository.latestFetchedAt() == 1000)
    }

    @Test("갱신 중에 조회해도 갱신이 실패하면 유효한 캐시를 반환한다")
    func 갱신_중_조회는_갱신이_실패해도_유효한_캐시를_반환한다() async throws {
        let remote = HeldFailingRemoteDataSource()
        let local = try ExchangeRateLocalDataSource.make(inMemory: true)
        try await local.replaceRates([Self.usd])
        let repository = ExchangeRateRepositoryImpl(
            remoteDataSource: remote,
            localDataSource: local,
            now: { Date(timeIntervalSince1970: Double(1000 + 11 * 60 * 60 * 1000) / 1000) },
        )

        let refresh = Task { try await repository.refreshExchangeRates() }
        await remote.waitUntilFetchStarted()
        let rate = Task { try await repository.exchangeRate(from: "USD", to: "KRW") }
        // 조회가 진행 중인 갱신을 기다리기 시작한 뒤에 갱신을 실패시킨다.
        try await Task.sleep(for: .milliseconds(100))
        await remote.release()

        await #expect(throws: ExchangeRateError.self) {
            try await refresh.value
        }
        #expect(try await rate.value == 1441.1)
        #expect(await remote.fetchCallCount == 1)
    }
}

/// 풀어 줄 때까지 원격 조회를 붙잡아 두었다가 오류를 던지는 스텁. 갱신이 진행 중인 상황을 만든다.
private actor HeldFailingRemoteDataSource: ExchangeRateRemoteDataSource {
    private var isReleased = false
    private var heldFetches: [CheckedContinuation<Void, Never>] = []
    private var fetchStartWaiters: [CheckedContinuation<Void, Never>] = []
    private(set) var fetchCallCount = 0

    func fetchExchangeRates() async throws -> [ExchangeRateData] {
        fetchCallCount += 1
        fetchStartWaiters.forEach { $0.resume() }
        fetchStartWaiters.removeAll()

        if !isReleased {
            await withCheckedContinuation { heldFetches.append($0) }
        }

        throw ExchangeRateError.networkUnavailable()
    }

    func waitUntilFetchStarted() async {
        guard fetchCallCount == 0 else { return }

        await withCheckedContinuation { fetchStartWaiters.append($0) }
    }

    func release() {
        isReleased = true
        heldFetches.forEach { $0.resume() }
        heldFetches.removeAll()
    }
}

/// 원격 호출 횟수를 기록하는 스텁. result가 nil이면 오류를 던진다.
private actor RemoteDataSourceStub: ExchangeRateRemoteDataSource {
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
