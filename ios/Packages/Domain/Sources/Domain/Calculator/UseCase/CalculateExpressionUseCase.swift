import Foundation

public struct CalculateExpressionUseCase: Sendable {
    public init() {}

    /// 사용자 산술 표현식을 정규화하고 계산한 뒤, 결과를 포맷된 문자열로 반환합니다.
    ///
    /// 입력값은 정규화됩니다.
    /// 공백과 줄바꿈은 제거되고, `×`는 `*`, `÷`는 `/`, `−`는 `-`로 변환됩니다.
    /// 또한 표현식 끝에 연산자가 남아 있다면 해당 연산자는 제거한 뒤, 남은 표현식을 계산합니다.
    ///
    /// - Parameter expression: 계산할 산술 표현식입니다.
    ///   숫자, `.`, `+`, `-`, `*`, `/`, 괄호를 포함할 수 있으며,
    ///   지역화된 연산자 기호(`×`, `÷`, `−`)와 공백 또는 줄바꿈도 포함될 수 있습니다.
    ///
    /// - Returns: 계산 결과를 다음과 같은 문자열로 반환합니다.
    ///   - 정규화 후 표현식이 비어 있으면 `"0"`
    ///   - 계산에 실패하면 `"Error"`
    ///   - 그 외에는 숫자 문자열
    ///     결과가 정수이면 정수 형태로 포맷하고,
    ///     일반적인 크기의 소수이면 소수점 이하 최대 10자리까지 표시하되 뒤의 0은 제거합니다.
    ///     값의 크기가 1e15 이상이거나 0이 아니면서 1e-10보다 작으면,
    ///     소수점 이하 10자리의 과학적 표기법으로 표시합니다.
    public func calculate(_ expression: String) -> String {
        let normalized = expression
            .replacingOccurrences(of: "\n", with: "")
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "×", with: "*")
            .replacingOccurrences(of: "÷", with: "/")
            .replacingOccurrences(of: "−", with: "-")

        if normalized.isEmpty { return "0" }

        // 마지막이 연산자면 제거
        let finalExpression: String = if let last = normalized.last, "+-*/".contains(last) {
            String(normalized.dropLast())
        } else {
            normalized
        }

        guard let result = Self.evaluate(finalExpression) else { return "Error" }

        // 15자리를 넘어가거나(10^15), 너무 작으면(10^-10) E 표기법 사용
        let absResult = abs(result)
        if absResult != 0.0, absResult >= 1e15 || absResult < 1e-10 {
            // 예: 1.2345678900E15 (소수점 10자리)
            return String(format: "%.10E", result).replacingOccurrences(of: ",", with: ".")
        } else if result.truncatingRemainder(dividingBy: 1.0) == 0.0 {
            return String(format: "%.0f", result)
        } else {
            var formatted = String(format: "%.10f", result)
            while formatted.hasSuffix("0") { formatted.removeLast() }
            if formatted.hasSuffix(".") { formatted.removeLast() }
            return formatted
        }
    }

    private struct ParseError: Error {}

    /// 재귀 하강 파서로 수식을 평가한다. 문법 오류가 있으면 nil을 반환한다.
    private static func evaluate(_ expression: String) -> Double? {
        let chars = Array(expression)
        var pos = -1
        var ch: Character?

        func nextChar() {
            pos += 1
            ch = pos < chars.count ? chars[pos] : nil
        }

        func eat(_ charToEat: Character) -> Bool {
            while ch == " " { nextChar() }
            if ch == charToEat {
                nextChar()
                return true
            }
            return false
        }

        func parseExpression() throws -> Double {
            var x = try parseTerm()
            while true {
                if eat("+") {
                    x += try parseTerm()
                } else if eat("-") {
                    x -= try parseTerm()
                } else {
                    return x
                }
            }
        }

        func parseTerm() throws -> Double {
            var x = try parseFactor()
            while true {
                if eat("*") {
                    x *= try parseFactor()
                } else if eat("/") {
                    x /= try parseFactor()
                } else {
                    return x
                }
            }
        }

        func parseFactor() throws -> Double {
            if eat("+") { return try parseFactor() }
            if eat("-") { return try -parseFactor() }

            let startPos = pos
            if eat("(") {
                let x = try parseExpression()
                _ = eat(")")
                return x
            }
            if let current = ch, current.isASCII, current.isNumber || current == "." {
                while let current = ch, current.isASCII, current.isNumber || current == "." {
                    nextChar()
                }
                guard let value = Double(String(chars[startPos..<pos])) else { throw ParseError() }
                return value
            }
            throw ParseError()
        }

        do {
            nextChar()
            let x = try parseExpression()
            if pos < chars.count { throw ParseError() }
            return x
        } catch {
            return nil
        }
    }
}
