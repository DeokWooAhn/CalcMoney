import Foundation
import Testing
@testable import Presentation

@Suite("AppStoreLink")
struct AppStoreLinkTests {
    @Test("앱 ID가 있으면 App Store 상세 페이지 링크를 만든다")
    func 앱_ID가_있으면_링크를_만든다() {
        let url = AppStoreLink.recommendURL(appID: "1234567890")

        #expect(url?.absoluteString == "https://apps.apple.com/app/id1234567890")
    }

    @Test("앱 ID가 없거나 비어 있으면 링크가 없어 추천하기 항목을 숨긴다")
    func 앱_ID가_없으면_링크가_없다() {
        #expect(AppStoreLink.recommendURL(appID: nil) == nil)
        #expect(AppStoreLink.recommendURL(appID: "") == nil)
    }

    @Test("앱 ID가 양의 정수가 아니면 링크를 만들지 않는다", arguments: ["0", "-1", "123?foo", "12 34", "abc"])
    func 양의_정수가_아니면_링크를_만들지_않는다(appID: String) {
        #expect(AppStoreLink.recommendURL(appID: appID) == nil)
    }
}
