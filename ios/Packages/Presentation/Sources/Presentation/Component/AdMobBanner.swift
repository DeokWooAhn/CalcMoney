import GoogleMobileAds
import SwiftUI

/// 화면 하단 AdMob 배너 (Android `AdMobBanner` 대응)
///
/// 들어갈 자리의 폭에 맞춘 앵커 적응형 배너를 쓴다. 고정 320×50보다 큰 광고가 들어와 수익이 낫고,
/// 높이는 기기에 따라 50~90pt로 SDK가 정한다. iPad처럼 창 폭이 바뀌면 새 크기로 배너를 다시 만든다.
/// 동의가 완료되지 않았으면 아무것도 그리지 않는다.
struct AdMobBanner: View {
    let adUnitID: String
    let canRequestAds: Bool

    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @State private var availableWidth: CGFloat = 0

    var body: some View {
        if canRequestAds {
            ZStack {
                if availableWidth > 0 {
                    let adSize = currentOrientationAnchoredAdaptiveBanner(width: availableWidth)

                    BannerAdRepresentable(adUnitID: adUnitID, adSize: adSize)
                        .frame(width: adSize.size.width, height: adSize.size.height)
                        // 폭이 바뀌면 이미 받은 광고의 크기를 바꿀 수 없어 배너를 새로 만든다 (Android `remember(adSize)` 대응).
                        .id(adSize.size.width)
                }
            }
            // 최대 폭만 주면 부모가 더 좁은 폭을 제안해도 자식(이전 크기의 배너) 폭에 머문다.
            // 최소 폭 0을 함께 줘야 창이 줄 때도 제안받은 폭을 따르고, 그 폭을 재서 배너를 다시 만든다.
            .frame(minWidth: 0, maxWidth: .infinity)
            .onGeometryChange(for: CGFloat.self) { proxy in
                proxy.size.width
            } action: { width in
                availableWidth = width
            }
            .padding(.bottom, tabBarClearance)
        }
    }

    /// iOS 26의 떠 있는 탭바는 safe area를 차지하지 않으므로 배너를 그 위로 올린다.
    ///
    /// iPad의 넓은 창(regular)은 탭바가 화면 위에 있어 띄울 필요가 없다. 좁은 창과 iPhone(세로 고정)만 띄운다.
    private var tabBarClearance: CGFloat {
        if #available(iOS 26.0, *), horizontalSizeClass == .compact {
            68
        } else {
            0
        }
    }
}

private struct BannerAdRepresentable: UIViewRepresentable {
    let adUnitID: String
    let adSize: AdSize

    func makeUIView(context: Context) -> BannerView {
        let bannerView = BannerView(adSize: adSize)
        bannerView.adUnitID = adUnitID
        bannerView.load(Request())

        return bannerView
    }

    func updateUIView(_ bannerView: BannerView, context: Context) {}
}
