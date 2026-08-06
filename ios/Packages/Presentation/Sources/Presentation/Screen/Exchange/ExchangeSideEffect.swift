/// 환율 화면 일회성 이벤트 (Android `ExchangeContract.SideEffect` 대응)
public enum ExchangeSideEffect: Equatable {
    case showSnackbar(message: String)
}
