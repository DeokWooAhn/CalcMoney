import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("CalculatorViewModel — 계산 직렬화")
struct CalculatorViewModelCalculateTests {
    private static func makeViewModel(recorder: CalculatorTestRecorder) -> CalculatorViewModel {
        CalculatorTestEnvironment.makeViewModel(
            history: FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(recorder: recorder),
            currencySelection: FakeCurrencySelectionRepository(recorder: recorder),
        )
    }

    @Test("= 를 연타해도 같은 수식이 두 번 저장되지 않는다")
    func 등호_연타는_기록을_중복_저장하지_않는다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.number("2")))

        // 앞선 계산이 addHistory 에서 중단된 사이에 두 번째 "=" 가 들어오는 상황.
        viewModel.send(.calculate)
        viewModel.send(.calculate)

        try await Task.sleep(for: .milliseconds(300))

        let saved = await recorder.savedHistories
        #expect(saved.map(\.expression) == ["1+2", "3+2"])
    }

    @Test("= 연타는 반복 연산으로 이어진다")
    func 등호_연타는_반복_연산으로_이어진다() async throws {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.number("2")))

        viewModel.send(.calculate)
        viewModel.send(.calculate)

        try await Task.sleep(for: .milliseconds(300))

        // 1+2=3, 이어서 반복 연산 +2 가 적용되어 5 가 된다.
        #expect(viewModel.state.expression == "5")
    }
}
