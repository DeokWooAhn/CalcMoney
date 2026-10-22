import Domain
import Foundation
import Testing
@testable import Presentation

@MainActor
@Suite("CalculatorViewModel — 계산 직렬화", .timeLimit(.minutes(1)))
struct CalculatorViewModelCalculateTests {
    private static func makeViewModel(recorder: CalculatorTestRecorder) -> CalculatorViewModel {
        CalculatorTestEnvironment.makeViewModel(
            history: FakeCalculatorHistoryRepository(recorder: recorder),
            exchangeRate: FakeExchangeRateRepository(recorder: recorder),
            currencySelection: FakeCurrencySelectionRepository(recorder: recorder),
        )
    }

    /// 계산이 성공할 때마다 기록이 한 건씩 늘어나므로, 기록 수로 "=" 두 번의 처리가 끝났는지 안다.
    private static func waitForCalculations(_ viewModel: CalculatorViewModel, count: Int) async {
        await waitUntil { viewModel.state.histories.count >= count }
    }

    @Test("= 를 연타해도 같은 수식이 두 번 저장되지 않는다")
    func 등호_연타는_기록을_중복_저장하지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.number("2")))

        // 앞선 계산이 addHistory 에서 중단된 사이에 두 번째 "=" 가 들어오는 상황.
        viewModel.send(.calculate)
        viewModel.send(.calculate)

        await Self.waitForCalculations(viewModel, count: 2)

        let saved = await recorder.savedHistories
        #expect(saved.map(\.expression) == ["1+2", "3+2"])
    }

    @Test("= 연타는 반복 연산으로 이어진다")
    func 등호_연타는_반복_연산으로_이어진다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("+")))
        viewModel.send(.input(.number("2")))

        viewModel.send(.calculate)
        viewModel.send(.calculate)

        await Self.waitForCalculations(viewModel, count: 2)

        // 1+2=3, 이어서 반복 연산 +2 가 적용되어 5 가 된다.
        #expect(viewModel.state.expression == "5")
    }

    @Test("큰 수 결과에서 = 를 다시 누르면 E 표기법 결과에 반복 연산이 이어진다")
    func 큰_수_결과에서_등호를_다시_누르면_반복_연산이_이어진다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        for _ in 0..<15 {
            viewModel.send(.input(.number("9")))
        }
        viewModel.send(.input(.operator("×")))
        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.number("0")))

        viewModel.send(.calculate)
        viewModel.send(.calculate)

        await Self.waitForCalculations(viewModel, count: 2)

        #expect(viewModel.state.expression == "1.0000000000E+17")
        #expect(!viewModel.state.isError)
    }

    @Test("0으로 나누면 오류로 처리하고 기록에 남기지 않는다")
    func 영으로_나누면_오류로_처리하고_기록에_남기지_않는다() async {
        let recorder = CalculatorTestRecorder()
        let viewModel = Self.makeViewModel(recorder: recorder)

        viewModel.send(.input(.number("1")))
        viewModel.send(.input(.operator("÷")))
        viewModel.send(.input(.number("0")))
        viewModel.send(.calculate)

        await waitUntil { viewModel.state.isError }

        #expect(viewModel.state.expression == "1÷0")
        #expect(viewModel.state.histories.isEmpty)
        #expect(await recorder.savedHistories.isEmpty)
    }
}
