/// AdMob 광고 단위 ID.
///
/// 디버그 빌드는 Google 공식 테스트 배너·전면 광고 ID를 쓴다.
/// 릴리스 빌드는 AdMob 콘솔에서 발급한 iOS 앱의 실제 ID를 써야 하고, 아직 넣지 않았으면 컴파일이 멈춘다
/// (테스트 광고가 스토어판에 나가는 것을 막는다. Android의 릴리스 시크릿 검증과 같은 역할).
/// 실제 ID를 넣을 때는 앱 타깃 빌드 설정 `GAD_APPLICATION_IDENTIFIER`(Release)도 함께 바꾼다.
enum AdConfig {
    #if DEBUG
        static let exchangeBannerUnitID = "ca-app-pub-3940256099942544/2934735716"
        static let favoriteBannerUnitID = "ca-app-pub-3940256099942544/2934735716"
        static let settingsBannerUnitID = "ca-app-pub-3940256099942544/2934735716"
        /// 계산기를 떠날 때 보여 주는 전면 광고
        static let calculatorInterstitialUnitID = "ca-app-pub-3940256099942544/4411468910"
    #else
        #error("AdMob iOS 광고 단위 ID 4개와 빌드 설정 GAD_APPLICATION_IDENTIFIER(Release)에 실제 값을 넣어야 릴리스 빌드가 된다")
    #endif
}
