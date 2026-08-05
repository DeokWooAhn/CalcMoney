import Domain
import Foundation

/// 환율 리포지토리 구현 (Android `ExchangeRateRepositoryImpl` 대응)
///
/// 캐시가 유효하면(12시간 TTL) 로컬 데이터를 쓰고, 만료됐으면 원격에서 받아 캐시를 갱신한다.
/// actor 격리 + 진행 중 갱신 Task 공유로 중복 갱신을 막는다.
public actor ExchangeRateRepositoryImpl: ExchangeRateRepository {
    private static let cacheTTLMillis = 12 * 60 * 60 * 1000

    private let remoteDataSource: any ExchangeRateRemoteDataSource
    private let localDataSource: ExchangeRateLocalDataSource
    private let now: @Sendable () -> Date
    private var inFlightRefresh: Task<[ExchangeRateData], any Error>?

    public init(
        remoteDataSource: any ExchangeRateRemoteDataSource,
        localDataSource: ExchangeRateLocalDataSource,
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.remoteDataSource = remoteDataSource
        self.localDataSource = localDataSource
        self.now = now
    }

    /// 기준 통화 1단위를 대상 통화 단위로 변환하는 배율을 반환한다.
    public func exchangeRate(from: String, to: String) async throws -> Double {
        if from == to { return 1.0 }

        let rates = try await fetchRatesIfNeeded()

        guard let fromRate = rates.rate(of: from) else {
            throw ExchangeRateError.rateNotFound(currencyCode: from)
        }
        guard let toRate = rates.rate(of: to) else {
            throw ExchangeRateError.rateNotFound(currencyCode: to)
        }

        return fromRate / toRate
    }

    public func latestRateDate() async throws -> String {
        try await localDataSource.latestRateDate()
    }

    public func latestFetchedAt() async throws -> Int {
        try await localDataSource.latestFetchedAt()
    }

    public func refreshExchangeRates() async throws {
        _ = try await refresh()
    }

    /// 지원 통화 목록을 반환한다. 기준 통화 KRW를 항상 포함하고 코드 기준으로 중복을 제거한다.
    public func supportedCurrencies() async throws -> [CurrencyInfo] {
        let rates = try await fetchRatesIfNeeded()

        var seenCodes = Set<String>()

        return ([krwCurrencyInfo()] + rates.map { $0.toCurrencyInfo() })
            .filter { seenCodes.insert($0.code).inserted }
    }

    /// 캐시가 유효하면 캐시를, 만료됐으면 원격 데이터를 가져와 캐시를 갱신해 반환한다.
    ///
    /// 원격 조회에 실패해도 캐시가 있으면 캐시를 반환하고, 캐시가 없으면 원래 오류를 던진다.
    private func fetchRatesIfNeeded() async throws -> [ExchangeRateData] {
        if let inFlightRefresh {
            return try await inFlightRefresh.value
        }

        let cached = try await localDataSource.cachedRates()
        let fetchedAt = try await localDataSource.latestFetchedAt()
        let nowMillis = Int(now().timeIntervalSince1970 * 1000)

        if !cached.isEmpty, nowMillis - fetchedAt < Self.cacheTTLMillis {
            return cached
        }

        do {
            return try await refresh()
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            if cached.isEmpty { throw error }

            return cached
        }
    }

    /// TTL과 무관하게 원격에서 환율을 받아 캐시를 교체한다. 진행 중인 갱신이 있으면 그 결과를 공유한다.
    private func refresh() async throws -> [ExchangeRateData] {
        if let inFlightRefresh {
            return try await inFlightRefresh.value
        }

        let remoteDataSource = remoteDataSource
        let localDataSource = localDataSource
        let task = Task {
            let valid = try await remoteDataSource.fetchExchangeRates()
            try await localDataSource.replaceRates(valid)

            return valid
        }

        inFlightRefresh = task
        defer { inFlightRefresh = nil }

        return try await task.value
    }
}
