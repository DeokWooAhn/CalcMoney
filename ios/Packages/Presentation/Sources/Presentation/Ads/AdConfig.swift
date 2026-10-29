import Foundation

/// AdMob 광고 단위 ID.
///
/// 디버그 빌드는 Google 공식 테스트 배너·전면 광고 ID를 쓴다.
/// 릴리스 빌드는 앱의 Info.plist에서 실제 ID를 읽는다. 저장소가 공개라 실제 ID는 git이 무시하는
/// `ios/CalcMoney/Config/AdMob.xcconfig`에만 있고, 빌드 설정을 거쳐 Info.plist에 들어간다.
/// 값이 없으면 앱 타깃의 "Check AdMob IDs" 빌드 단계가 릴리스 빌드를 멈춘다
/// (테스트 광고가 스토어판에 나가는 것을 막는다. Android의 릴리스 시크릿 검증과 같은 역할).
enum AdConfig {
    #if DEBUG
        // 배너는 앵커 적응형이라 Google의 적응형 배너 테스트 ID를 쓴다 (Android 디버그 배너 ID와 같은 종류).
        static let exchangeBannerUnitID = "ca-app-pub-3940256099942544/2435281174"
        static let favoriteBannerUnitID = "ca-app-pub-3940256099942544/2435281174"
        static let settingsBannerUnitID = "ca-app-pub-3940256099942544/2435281174"
        /// 계산기를 떠날 때 보여 주는 전면 광고
        static let calculatorInterstitialUnitID = "ca-app-pub-3940256099942544/4411468910"
    #else
        // 키는 앱의 Info.plist와 같다.
        static let exchangeBannerUnitID = infoValue("AdMobExchangeBannerID")
        static let favoriteBannerUnitID = infoValue("AdMobFavoriteBannerID")
        static let settingsBannerUnitID = infoValue("AdMobSettingsBannerID")
        static let calculatorInterstitialUnitID = infoValue("AdMobCalculatorInterstitialID")

        private static func infoValue(_ key: String) -> String {
            Bundle.main.object(forInfoDictionaryKey: key) as? String ?? ""
        }
    #endif
}
