/// 계산기 화면 일회성 이벤트 (Android `CalculatorContract.SideEffect` 대응)
public enum CalculatorSideEffect: Equatable {
    case showSnackbar(message: String)
    /// 인앱 리뷰 창을 요청한다. 창을 띄울지는 OS가 정한다.
    case requestReview
}
