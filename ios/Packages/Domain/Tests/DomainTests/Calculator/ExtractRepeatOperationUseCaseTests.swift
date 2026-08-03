import Testing
@testable import Domain

@Suite("ExtractRepeatOperationUseCase — 반복 연산 추출")
struct ExtractRepeatOperationUseCaseTests {
    private let useCase = ExtractRepeatOperationUseCase()

    @Test("덧셈 수식에서 마지막 연산과 피연산자를 추출한다")
    func 덧셈_추출() {
        #expect(useCase("2+1") == "+1")
    }

    @Test("뺄셈 수식에서 마지막 연산과 피연산자를 추출한다")
    func 뺄셈_추출() {
        #expect(useCase("10-3") == "-3")
    }

    @Test("곱셈 수식에서 마지막 연산과 피연산자를 추출한다")
    func 곱셈_추출() {
        #expect(useCase("4×5") == "×5")
    }

    @Test("나눗셈 수식에서 마지막 연산과 피연산자를 추출한다")
    func 나눗셈_추출() {
        #expect(useCase("10÷2") == "÷2")
    }

    @Test("음수 피연산자도 반복 연산에 포함한다")
    func 음수_피연산자() {
        #expect(useCase("2×-1") == "×-1")
    }

    @Test("유니코드 음수 피연산자도 반복 연산에 포함한다")
    func 유니코드_음수_피연산자() {
        #expect(useCase("2×−1") == "×−1")
    }

    @Test("연산자가 없으면 nil을 반환한다")
    func 연산자_없음() {
        #expect(useCase("100") == nil)
    }

    @Test("연산자로 끝나는 수식이면 nil을 반환한다")
    func 연산자로_끝남() {
        #expect(useCase("10+") == nil)
    }

    @Test("오른쪽 피연산자가 숫자가 아니면 nil을 반환한다")
    func 피연산자가_숫자_아님() {
        #expect(useCase("10+abc") == nil)
    }

    @Test("첫 글자가 연산자인 단일 음수는 nil을 반환한다")
    func 단일_음수() {
        #expect(useCase("-10") == nil)
    }

    @Test("첫 글자가 유니코드 음수인 단일 음수는 nil을 반환한다")
    func 유니코드_단일_음수() {
        #expect(useCase("−10") == nil)
    }
}
