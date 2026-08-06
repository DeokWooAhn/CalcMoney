import Testing
@testable import CalcMoney

@Suite("AppContainer")
struct AppContainerTests {
    @Test("컨테이너는 계산기 ViewModel을 초기 상태로 조립한다")
    @MainActor
    func 컨테이너는_계산기_ViewModel을_초기_상태로_조립한다() {
        let container = AppContainer()

        #expect(container.calculatorViewModel.state.expression.isEmpty)
        #expect(container.calculatorViewModel.state.cursorPosition == 0)
    }
}
