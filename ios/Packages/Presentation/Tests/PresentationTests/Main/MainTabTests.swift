import Testing
@testable import Presentation

@Suite("MainTab")
struct MainTabTests {
    @Test("탭은 계산기·환율·즐겨찾기·설정 순서로 4개다")
    func 탭은_4개다() {
        #expect(MainTab.allCases == [.calculator, .exchange, .favorite, .setting])
    }
}
