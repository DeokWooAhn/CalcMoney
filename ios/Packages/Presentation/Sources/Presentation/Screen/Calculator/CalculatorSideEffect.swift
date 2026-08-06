/// 계산기 화면 일회성 이벤트 (Android `CalculatorContract.SideEffect` 대응)
public enum CalculatorSideEffect: Equatable {
    case showSnackbar(message: String)
}
