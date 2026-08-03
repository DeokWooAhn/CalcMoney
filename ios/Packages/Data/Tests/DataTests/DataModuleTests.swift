import Testing
@testable import Data

@Suite("DataModule")
struct DataModuleTests {
    @Test("기본 테마 모드는 시스템 설정을 따른다")
    func 기본_테마_모드는_시스템_설정을_따른다() {
        #expect(DataModule.defaultThemeMode == .system)
    }
}
