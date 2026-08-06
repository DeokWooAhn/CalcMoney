import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("CalculatorViewModel — 연산자 입력 방어")
struct CalculatorViewModelOperatorInputTests {
    private static func makeViewModel() -> CalculatorViewModel {
        let recorder = CalculatorTestRecorder()

        return CalculatorTestEnvironment.makeViewModel(
            history: FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(recorder: recorder),
            currencySelection: FakeCurrencySelectionRepository(recorder: recorder),
        )
    }

    /// `1+` 까지 입력해 연산자 치환 분기에 걸리는 상태를 만든다.
    private static func makeViewModelWithTrailingOperator() -> CalculatorViewModel {
        let viewModel = makeViewModel()
        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))

        return viewModel
    }

    @Test("빈 연산자 문자열은 무시된다")
    func 빈_연산자_문자열은_무시된다() {
        let viewModel = Self.makeViewModelWithTrailingOperator()

        // Character("") 는 런타임 트랩이라 방어 전에는 여기서 프로세스가 죽었다.
        viewModel.send(.input(.operator("")))

        #expect(viewModel.state.expression == "1+")
    }

    @Test("두 글자 이상 연산자 문자열은 무시된다")
    func 여러_글자_연산자_문자열은_무시된다() {
        let viewModel = Self.makeViewModelWithTrailingOperator()

        viewModel.send(.input(.operator("**")))

        #expect(viewModel.state.expression == "1+")
    }

    @Test("연산자가 아닌 한 글자도 무시된다")
    func 연산자가_아닌_문자는_무시된다() {
        let viewModel = Self.makeViewModelWithTrailingOperator()

        viewModel.send(.input(.operator("%")))

        #expect(viewModel.state.expression == "1+")
    }

    @Test("정상 연산자는 앞선 연산자를 치환한다")
    func 정상_연산자는_앞선_연산자를_치환한다() {
        let viewModel = Self.makeViewModelWithTrailingOperator()

        viewModel.send(.input(.operator("×")))

        #expect(viewModel.state.expression == "1×")
        #expect(viewModel.state.cursorPosition == 2)
    }

    @Test("음수 입력은 연산자 뒤에 그대로 붙는다")
    func 음수_입력은_연산자_뒤에_붙는다() {
        let viewModel = Self.makeViewModelWithTrailingOperator()

        viewModel.send(.input(.operator("-")))

        #expect(viewModel.state.expression == "1+-")
    }
}
