/// AdMob 광고 단위 ID.
///
/// 현재는 Google 공식 테스트 배너 ID를 사용한다.
/// 릴리스 전에 AdMob 콘솔에서 발급한 실제 ID로 교체해야 한다 (Info.plist의 GADApplicationIdentifier도 함께).
enum AdConfig {
    static let exchangeBannerUnitID = "ca-app-pub-3940256099942544/2934735716"
    static let favoriteBannerUnitID = "ca-app-pub-3940256099942544/2934735716"
    static let settingsBannerUnitID = "ca-app-pub-3940256099942544/2934735716"
}
