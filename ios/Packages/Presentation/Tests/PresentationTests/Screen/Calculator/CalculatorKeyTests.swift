import Testing
@testable import Presentation

@Suite("CalculatorKey")
struct CalculatorKeyTests {
    @Test("숫자 키는 숫자 토큰 입력으로 변환된다")
    func 숫자_키는_숫자_토큰_입력으로_변환된다() {
        #expect(CalculatorKey.number("7").toIntent() == .input(.number("7")))
    }

    @Test("연산자 키는 화면 표시 문자가 아니라 입력값으로 변환된다")
    func 연산자_키는_화면_표시_문자가_아니라_입력값으로_변환된다() {
        let key = CalculatorKey.operatorKey(displayText: "−", inputValue: "-")

        #expect(key.toIntent() == .input(.operator("-")))
    }

    @Test("나누기 키는 입력값도 나눗셈 기호를 유지한다")
    func 나누기_키는_입력값도_나눗셈_기호를_유지한다() {
        let key = CalculatorKey.operatorKey(displayText: "÷", inputValue: "÷")

        #expect(key.toIntent() == .input(.operator("÷")))
    }

    @Test("소수점 키는 소수점 토큰 입력으로 변환된다")
    func 소수점_키는_소수점_토큰_입력으로_변환된다() {
        #expect(CalculatorKey.dot.toIntent() == .input(.dot))
    }

    @Test("괄호 키는 괄호 토큰 입력으로 변환된다")
    func 괄호_키는_괄호_토큰_입력으로_변환된다() {
        #expect(CalculatorKey.parenthesis.toIntent() == .input(.parenthesis))
    }

    @Test("AC 키는 clear로 변환된다")
    func AC_키는_clear로_변환된다() {
        #expect(CalculatorKey.clear.toIntent() == .clear)
    }

    @Test("지우기 키는 delete로 변환된다")
    func 지우기_키는_delete로_변환된다() {
        #expect(CalculatorKey.delete.toIntent() == .delete)
    }

    @Test("등호 키는 calculate로 변환된다")
    func 등호_키는_calculate로_변환된다() {
        #expect(CalculatorKey.calculate.toIntent() == .calculate)
    }

    @Test("기록 키는 ViewModel 의도가 아니므로 변환되지 않는다")
    func 기록_키는_ViewModel_의도가_아니므로_변환되지_않는다() {
        #expect(CalculatorKey.history.toIntent() == nil)
    }

    @Test("키패드는 5행 4열이다")
    func 키패드는_5행_4열이다() {
        #expect(calculatorKeyRows.count == 5)
        #expect(calculatorKeyRows.allSatisfy { $0.count == 4 })
    }

    @Test("기록 키를 뺀 모든 키가 의도로 변환된다")
    func 기록_키를_뺀_모든_키가_의도로_변환된다() {
        let keys = calculatorKeyRows.flatMap(\.self).filter { $0 != .history }

        #expect(keys.allSatisfy { $0.toIntent() != nil })
    }

    @Test("기록 키는 첫 행에 하나만 있다")
    func 기록_키는_첫_행에_하나만_있다() {
        let historyCount = calculatorKeyRows.flatMap(\.self).count { $0 == .history }

        #expect(historyCount == 1)
        #expect(calculatorKeyRows.first?.first == .history)
    }

    /// Maestro Flow가 이 값으로 키를 찾고, Android CalculatorKey.testTag와 같은 값이어야
    /// Flow 하나로 두 플랫폼을 돌릴 수 있다. 값을 바꾸면 .maestro Flow와 Android도 함께 바꿔야 한다.
    @Test("키패드 배열 순서대로 약속한 E2E id를 가진다")
    func 키패드_배열_순서대로_약속한_E2E_id를_가진다() {
        let ids = calculatorKeyRows.flatMap(\.self).map(\.testID)

        #expect(ids == [
            "keypad.history", "keypad.clear", "keypad.parenthesis", "keypad.divide",
            "keypad.7", "keypad.8", "keypad.9", "keypad.multiply",
            "keypad.4", "keypad.5", "keypad.6", "keypad.minus",
            "keypad.1", "keypad.2", "keypad.3", "keypad.plus",
            "keypad.dot", "keypad.0", "keypad.delete", "keypad.equals",
        ])
    }

    /// Maestro는 id를 정규식으로 해석한다. 기호가 섞이면 다른 키와 매칭된다.
    @Test("모든 키의 E2E id가 정규식 특수문자 없이 영문 소문자와 숫자로만 이뤄진다")
    func 모든_키의_E2E_id가_정규식_특수문자_없이_영문_소문자와_숫자로만_이뤄진다() {
        let ids = calculatorKeyRows.flatMap(\.self).map(\.testID)

        #expect(ids.allSatisfy { $0.wholeMatch(of: /keypad\.[a-z0-9]+/) != nil })
    }

    @Test("모든 키의 E2E id가 서로 다르다")
    func 모든_키의_E2E_id가_서로_다르다() {
        let ids = calculatorKeyRows.flatMap(\.self).map(\.testID)

        #expect(Set(ids).count == ids.count)
    }
}
