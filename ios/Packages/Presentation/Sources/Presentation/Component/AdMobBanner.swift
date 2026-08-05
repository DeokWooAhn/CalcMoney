import GoogleMobileAds
import SwiftUI

/// 화면 하단 AdMob 배너 (Android `AdMobBanner` 대응)
///
/// 동의가 완료되지 않았으면 아무것도 그리지 않는다.
struct AdMobBanner: View {
    let adUnitID: String
    let canRequestAds: Bool

    var body: some View {
        if canRequestAds {
            BannerAdRepresentable(adUnitID: adUnitID)
                .frame(width: 320, height: 50)
                .frame(maxWidth: .infinity)
                .padding(.bottom, tabBarClearance)
        }
    }

    /// iOS 26의 떠 있는 탭바는 safe area를 차지하지 않으므로 배너를 그 위로 올린다.
    private var tabBarClearance: CGFloat {
        if #available(iOS 26.0, *) { 68 } else { 0 }
    }
}

private struct BannerAdRepresentable: UIViewRepresentable {
    let adUnitID: String

    func makeUIView(context: Context) -> BannerView {
        let bannerView = BannerView(adSize: AdSizeBanner)
        bannerView.adUnitID = adUnitID
        bannerView.load(Request())

        return bannerView
    }

    func updateUIView(_ bannerView: BannerView, context: Context) {}
}
