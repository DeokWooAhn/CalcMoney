import Domain

/// 원격 환율 데이터소스 프로토콜.
///
/// Firestore를 직접 만지는 부분을 이 프로토콜 뒤에 두어,
/// 리포지토리 테스트가 Firebase 없이 스텁으로 동작하게 한다.
public protocol ExchangeRateRemoteDataSource: Sendable {
    func fetchExchangeRates() async throws -> [ExchangeRateData]
}

/// Firebase가 아직 설정되지 않았을 때(GoogleService-Info.plist 없음) 쓰는 대체 구현.
public struct UnavailableExchangeRateRemoteDataSource: ExchangeRateRemoteDataSource {
    public init() {}

    public func fetchExchangeRates() async throws -> [ExchangeRateData] {
        throw ExchangeRateError.notReady()
    }
}
