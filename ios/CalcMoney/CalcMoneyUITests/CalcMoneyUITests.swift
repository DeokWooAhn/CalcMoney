import XCTest

/// 핵심 사용자 흐름 스모크 테스트.
///
/// 지역화와 무관하게 동작하도록 한국어를 강제해 실행한다.
final class CalcMoneyUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    @MainActor
    private func launchApp() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-AppleLanguages", "(ko)", "-AppleLocale", "ko_KR"]
        app.launch()

        return app
    }

    @MainActor
    func test계산기에서_기본_계산이_동작한다() throws {
        let app = launchApp()

        app.buttons["keypad.7"].tap()
        app.buttons["keypad.+"].tap()
        app.buttons["keypad.7"].tap()
        app.buttons["keypad.="].tap()

        XCTAssertTrue(
            app.staticTexts["14"].waitForExistence(timeout: 5),
            "7+7 계산 결과 14가 표시되어야 한다",
        )
    }

    @MainActor
    func test계산_기록_패널이_열린다() throws {
        let app = launchApp()

        app.buttons["계산 기록"].tap()

        XCTAssertTrue(
            app.staticTexts["계산 기록"].waitForExistence(timeout: 5),
            "계산 기록 패널이 표시되어야 한다",
        )
    }

    @MainActor
    func test환율_탭으로_전환하면_기준_금액_입력이_보인다() throws {
        let app = launchApp()

        app.tabBars.buttons["환율"].tap()

        XCTAssertTrue(
            app.staticTexts["기준 금액"].waitForExistence(timeout: 10),
            "환율 화면의 기준 금액 라벨이 표시되어야 한다",
        )
        XCTAssertTrue(
            app.staticTexts["받을 금액"].exists,
            "환율 화면의 받을 금액 라벨이 표시되어야 한다",
        )
    }

    @MainActor
    func test설정_탭에서_테마_카드가_보인다() throws {
        let app = launchApp()

        app.tabBars.buttons["설정"].tap()

        XCTAssertTrue(
            app.staticTexts["테마"].waitForExistence(timeout: 5),
            "설정 화면의 테마 카드가 표시되어야 한다",
        )
        XCTAssertTrue(
            app.staticTexts["버전"].exists,
            "설정 화면의 버전 항목이 표시되어야 한다",
        )
    }
}
