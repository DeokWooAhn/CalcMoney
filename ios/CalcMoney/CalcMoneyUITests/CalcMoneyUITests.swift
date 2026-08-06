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
        // UMP 동의 흐름을 끈다. AdConsentManager.uiTestingLaunchArgument와 값이 같아야 한다.
        // 하이픈으로 시작하면 UserDefaults가 키-값으로 해석해 뒤 인자를 삼키므로 붙이지 않는다.
        app.launchArguments.append("UI_TESTING")
        app.launch()

        return app
    }

    @MainActor
    func test계산기에서_기본_계산이_동작한다() {
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

    /// 지우기 키는 반복 삭제 때문에 Button이 아니라 Text + DragGesture로 만들어져 있다.
    /// 접근성 특성과 활성화 동작을 붙이지 않으면 VoiceOver 사용자가 삭제를 아예 할 수 없다.
    @MainActor
    func test지우기_키가_버튼으로_노출되고_활성화하면_한_글자를_지운다() {
        let app = launchApp()

        app.buttons["keypad.7"].tap()
        app.buttons["keypad.8"].tap()

        XCTAssertTrue(
            app.staticTexts["78"].waitForExistence(timeout: 5),
            "수식 78이 표시되어야 한다",
        )

        let deleteKey = app.buttons["지우기"]
        XCTAssertTrue(
            deleteKey.waitForExistence(timeout: 5),
            "지우기 키가 접근성 트리에 버튼으로 노출되어야 한다",
        )

        deleteKey.tap()

        XCTAssertTrue(
            app.staticTexts["7"].waitForExistence(timeout: 5),
            "한 글자가 지워져 수식이 7이 되어야 한다",
        )
        XCTAssertFalse(
            app.staticTexts["78"].exists,
            "지우기 후에는 수식 78이 남아 있으면 안 된다",
        )
    }

    @MainActor
    func test계산_기록_패널이_열린다() {
        let app = launchApp()

        app.buttons["계산 기록"].tap()

        XCTAssertTrue(
            app.staticTexts["계산 기록"].waitForExistence(timeout: 5),
            "계산 기록 패널이 표시되어야 한다",
        )
    }

    /// 아이콘만 있던 닫기 버튼에 이름과 최소 터치 영역(44pt)을 붙였다.
    /// 라벨이 없으면 VoiceOver가 읽지 못하고, 이 조회 자체도 실패한다.
    @MainActor
    func test계산_기록_패널을_닫기_버튼으로_닫는다() {
        let app = launchApp()

        app.buttons["계산 기록"].tap()

        let panelTitle = app.staticTexts["계산 기록"]
        XCTAssertTrue(panelTitle.waitForExistence(timeout: 5), "계산 기록 패널이 표시되어야 한다")

        let closeButton = app.buttons["계산 기록 닫기"]
        XCTAssertTrue(closeButton.waitForExistence(timeout: 5), "닫기 버튼이 이름을 가진 버튼으로 노출되어야 한다")

        closeButton.tap()

        XCTAssertFalse(
            panelTitle.waitForExistence(timeout: 2),
            "닫기 버튼을 누르면 계산 기록 패널이 사라져야 한다",
        )
    }

    @MainActor
    func test환율_탭으로_전환하면_기준_금액_입력이_보인다() {
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
    func test설정_탭에서_테마_카드가_보인다() {
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
