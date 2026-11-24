import Domain
import Foundation

/// 환율 리포지토리 구현 (Android `ExchangeRateRepositoryImpl` 대응)
///
/// 캐시가 유효하면(12시간 TTL) 로컬 데이터를 쓰고, 만료됐으면 원격에서 받아 캐시를 갱신한다.
/// actor 격리 + 진행 중 갱신 Task 공유로 중복 갱신을 막는다.
/// 만료된 캐시가 있으면 원격 조회는 5분에 한 번만 다시 시도한다(`docs/decisions/exchange-rate-remote-retry-cooldown.md`).
public actor ExchangeRateRepositoryImpl: ExchangeRateRepository {
    private static let cacheTTLMillis = 12 * 60 * 60 * 1000
    /// 캐시가 만료된 뒤 원격 조회를 다시 시도하기까지 기다리는 시간. Android `REMOTE_RETRY_COOLDOWN_MS`와 같은 값.
    private static let remoteRetryCooldownMillis = 5 * 60 * 1000

    private let remoteDataSource: any ExchangeRateRemoteDataSource
    private let localDataSource: ExchangeRateLocalDataSource
    private let crashBreadcrumbs: any CrashBreadcrumbs
    private let now: @Sendable () -> Date
    private var inFlightRefresh: Task<[ExchangeRateData], any Error>?
    /// 마지막으로 끝난 원격 조회를 시작한 시각(성공·실패 무관). 앱을 다시 켜면 비운다.
    private var lastRemoteAttemptMillis: Int?

    public init(
        remoteDataSource: any ExchangeRateRemoteDataSource,
        localDataSource: ExchangeRateLocalDataSource,
        crashBreadcrumbs: any CrashBreadcrumbs = NoOpCrashBreadcrumbs(),
        now: @escaping @Sendable () -> Date = { Date() },
    ) {
        self.remoteDataSource = remoteDataSource
        self.localDataSource = localDataSource
        self.crashBreadcrumbs = crashBreadcrumbs
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

    /// 사용자가 요청한 새로고침이라 쿨다운과 관계없이 원격을 부른다.
    public func refreshExchangeRates() async throws {
        do {
            let valid = try await refresh()
            crashBreadcrumbs.log("rates: manual refresh \(valid.count)")
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            crashBreadcrumbs.log("rates: manual refresh failed (\(error.breadcrumbName))")
            throw error
        }
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
    /// 진행 중인 갱신이 있어도 캐시부터 본다. 유효하면 갱신을 기다리지 않고, 만료됐으면 그 갱신을 함께 기다린다.
    ///
    /// 캐시가 만료됐어도 마지막 원격 조회에서 5분이 지나지 않았으면 원격을 부르지 않고 캐시를 돌려준다.
    /// `fetchedAt`은 서버 문서가 갱신된 시각이라 원격 조회가 성공해도 만료 상태가 이어질 수 있고,
    /// 오프라인에서는 실패가 이어지기 때문이다. 캐시가 비어 있으면 돌려줄 것이 없으므로 매번 원격을 시도한다.
    private func fetchRatesIfNeeded() async throws -> [ExchangeRateData] {
        let cached = try await localDataSource.cachedRates()
        let fetchedAt = try await localDataSource.latestFetchedAt()
        let nowMillis = currentMillis()

        if !cached.isEmpty, nowMillis - fetchedAt < Self.cacheTTLMillis || isInRemoteRetryCooldown(nowMillis) {
            return cached
        }

        do {
            let valid = try await refresh()
            crashBreadcrumbs.log("rates: refreshed \(valid.count) from remote")

            return valid
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            let fallback = cached.isEmpty ? "no cache" : "using \(cached.count) cached"
            crashBreadcrumbs.log("rates: remote failed (\(error.breadcrumbName)), \(fallback)")

            if cached.isEmpty { throw error }

            return cached
        }
    }

    /// TTL과 무관하게 원격에서 환율을 받아 캐시를 교체한다. 진행 중인 갱신이 있으면 그 결과를 공유한다.
    ///
    /// 시도 시각은 갱신이 끝난 뒤에 기록한다. 진행 중에 기록하면 그동안 들어온 만료 캐시 조회가
    /// 갱신을 기다리지 않고 쿨다운에 걸려 옛 캐시를 받는다.
    private func refresh() async throws -> [ExchangeRateData] {
        if let inFlightRefresh {
            return try await inFlightRefresh.value
        }

        let startedAt = currentMillis()
        let remoteDataSource = remoteDataSource
        let localDataSource = localDataSource
        let task = Task {
            let valid = try await remoteDataSource.fetchExchangeRates()
            try await localDataSource.replaceRates(valid)

            return valid
        }

        inFlightRefresh = task
        defer {
            inFlightRefresh = nil
            lastRemoteAttemptMillis = startedAt
        }

        return try await task.value
    }

    /// 기기 시각이 뒤로 돌아간 경우(음수 경과)는 쿨다운으로 보지 않는다.
    private func isInRemoteRetryCooldown(_ nowMillis: Int) -> Bool {
        guard let lastRemoteAttemptMillis else { return false }

        let elapsed = nowMillis - lastRemoteAttemptMillis

        return elapsed >= 0 && elapsed < Self.remoteRetryCooldownMillis
    }

    private func currentMillis() -> Int {
        Int(now().timeIntervalSince1970 * 1000)
    }
}

private extension Error {
    /// 크래시 단서에 남길 오류 이름. 원인 오류의 메시지는 빼고 종류만 남긴다.
    var breadcrumbName: String {
        if let error = self as? ExchangeRateError {
            switch error {
            case .notReady: return "notReady"
            case .networkUnavailable: return "networkUnavailable"
            case .temporarilyUnavailable: return "temporarilyUnavailable"
            case .rateNotFound: return "rateNotFound"
            }
        }

        let nsError = self as NSError

        return "\(nsError.domain) \(nsError.code)"
    }
}
