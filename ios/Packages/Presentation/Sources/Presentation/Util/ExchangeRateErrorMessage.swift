import Domain

/// 환율 오류를 사용자 메시지로 변환한다 (Android `ExchangeRateErrorMapper` 대응)
extension Error {
    var exchangeRateErrorMessage: String {
        switch self as? ExchangeRateError {
        case .notReady:
            L("오늘 환율 정보가 아직 고시되지 않았습니다. 영업일 11시 이후 다시 확인해 주세요.")

        case .networkUnavailable:
            L("인터넷 연결이 없어 환율 정보를 불러올 수 없습니다. 연결 상태를 확인해 주세요.")

        case .rateNotFound:
            L("선택한 통화의 환율 정보를 찾을 수 없습니다.")

        case .temporarilyUnavailable, nil:
            L("환율 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.")
        }
    }
}
