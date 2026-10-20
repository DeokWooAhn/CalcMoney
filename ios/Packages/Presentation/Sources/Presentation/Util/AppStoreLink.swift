import Foundation

/// 설정의 "친구에게 추천하기"가 공유하는 App Store 링크 (Android `playStoreRecommendUrl` 대응)
enum AppStoreLink {
    /// App Store Connect의 앱 Apple ID(숫자). 아직 App Store에 출시하지 않아 없다.
    ///
    /// 출시 후 채우면 설정 화면에 "친구에게 추천하기"가 나타난다. 그 전에는 공유할 링크가 없어 항목을 숨긴다.
    static let appID: String? = nil

    static var recommendURL: URL? {
        recommendURL(appID: appID)
    }

    /// App Store 앱 ID는 양의 정수다. 잘못 넣은 값(음수, 0, 숫자 뒤에 붙은 문자)으로 엉뚱한 링크를 공유하지 않게 거른다.
    static func recommendURL(appID: String?) -> URL? {
        guard let appID, let number = UInt64(appID), number > 0 else { return nil }

        return URL(string: "https://apps.apple.com/app/id\(number)")
    }
}
