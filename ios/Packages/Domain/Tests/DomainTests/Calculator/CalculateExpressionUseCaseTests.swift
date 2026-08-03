import Testing
@testable import Domain

@Suite("CalculateExpressionUseCase")
struct CalculateExpressionUseCaseTests {
    private let engine = CalculateExpressionUseCase()

    @Suite("기본 사칙연산")
    struct 기본사칙연산 {
        private let engine = CalculateExpressionUseCase()

        @Test("덧셈: 1+2 = 3")
        func 덧셈() {
            #expect(engine.calculate("1+2") == "3")
        }

        @Test("뺄셈: 10-3 = 7")
        func 뺄셈() {
            #expect(engine.calculate("10-3") == "7")
        }

        @Test("곱셈: 4×5 = 20")
        func 곱셈() {
            #expect(engine.calculate("4×5") == "20")
        }

        @Test("나눗셈: 10÷2 = 5")
        func 나눗셈() {
            #expect(engine.calculate("10÷2") == "5")
        }
    }

    @Suite("소수점 계산")
    struct 소수점계산 {
        private let engine = CalculateExpressionUseCase()

        @Test("1.5+2.5 = 4")
        func 소수_덧셈() {
            #expect(engine.calculate("1.5+2.5") == "4")
        }

        @Test("10÷3은 소수점을 포함해야 한다")
        func 소수점_포함() {
            #expect(engine.calculate("10÷3") == "3.3333333333")
        }
    }

    @Suite("괄호 연산")
    struct 괄호연산 {
        private let engine = CalculateExpressionUseCase()

        @Test("(1+2)×3 = 9")
        func 괄호() {
            #expect(engine.calculate("(1+2)×3") == "9")
        }

        @Test("중첩 괄호: ((2+3))×2 = 10")
        func 중첩_괄호() {
            #expect(engine.calculate("((2+3))×2") == "10")
        }
    }

    @Suite("엣지 케이스")
    struct 엣지케이스 {
        private let engine = CalculateExpressionUseCase()

        @Test("빈 문자열은 0을 반환한다")
        func 빈_문자열() {
            #expect(engine.calculate("") == "0")
        }

        @Test("마지막이 연산자면 무시한다")
        func 마지막_연산자_무시() {
            #expect(engine.calculate("10+") == "10")
        }

        @Test("잘못된 수식은 Error를 반환한다")
        func 잘못된_수식() {
            #expect(engine.calculate("++") == "Error")
        }

        @Test("매우 큰 수는 E 표기법을 사용한다")
        func 큰_수_E_표기법() {
            #expect(engine.calculate("999999999999999×10") == "1.0000000000E+16")
        }
    }
}
