/// 환율 데이터 조회 실패 원인 (Android `ExchangeRateException` 대응)
public enum ExchangeRateError: Error {
    /// Cloud Functions가 아직 환율 문서를 만들지 않은 상태
    case notReady(cause: (any Error)? = nil)
    /// 네트워크에 연결할 수 없는 상태
    case networkUnavailable(cause: (any Error)? = nil)
    /// 일시적으로 데이터를 가져올 수 없는 상태
    case temporarilyUnavailable(cause: (any Error)? = nil)
    /// 요청한 통화 코드의 환율이 없는 상태
    case rateNotFound(currencyCode: String, cause: (any Error)? = nil)
}
