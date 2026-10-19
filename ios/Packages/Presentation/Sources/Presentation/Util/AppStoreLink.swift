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

    static func recommendURL(appID: String?) -> URL? {
        guard let appID, !appID.isEmpty else { return nil }

        return URL(string: "https://apps.apple.com/app/id\(appID)")
    }
}
