import Testing
@testable import Domain

@Suite("ThemeMode")
struct ThemeModeTests {
    @Test("세 가지 테마 모드를 제공한다")
    func 세가지_테마_모드를_제공한다() {
        #expect(ThemeMode.allCases == [.system, .light, .dark])
    }
}
