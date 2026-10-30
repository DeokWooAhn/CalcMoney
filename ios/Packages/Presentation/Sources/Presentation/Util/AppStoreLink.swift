import Foundation

/// 설정의 "친구에게 추천하기"가 공유하는 App Store 링크 (Android `playStoreRecommendUrl` 대응)
enum AppStoreLink {
    /// App Store Connect의 앱 Apple ID(숫자). 앱 정보 → 일반 정보의 "Apple ID"다.
    ///
    /// 값이 있으면 설정 화면에 "친구에게 추천하기"가 나타나고, 없으면 공유할 링크가 없어 항목을 숨긴다.
    /// 출시 전(TestFlight)에는 링크가 열리지 않지만, 첫 버전부터 항목을 보여 주려고 앱을 만들 때 받은 값을 미리 넣었다.
    static let appID: String? = "6828736184"

    static var recommendURL: URL? {
        recommendURL(appID: appID)
    }

    /// App Store 앱 ID는 양의 정수다. 잘못 넣은 값(음수, 0, 숫자 뒤에 붙은 문자)으로 엉뚱한 링크를 공유하지 않게 거른다.
    static func recommendURL(appID: String?) -> URL? {
        guard let appID, let number = UInt64(appID), number > 0 else { return nil }

        return URL(string: "https://apps.apple.com/app/id\(number)")
    }
}
