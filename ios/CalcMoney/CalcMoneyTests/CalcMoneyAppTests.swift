import Domain
import Testing
@testable import CalcMoney

@Suite("AppContainer")
struct AppContainerTests {
    @Test("컨테이너는 시스템 테마를 기본값으로 조립한다")
    func 컨테이너는_시스템_테마를_기본값으로_조립한다() {
        #expect(AppContainer().defaultThemeMode == .system)
    }
}
