import GoogleMobileAds
import OSLog
import UIKit

/// 전면 광고를 미리 받아 두었다가 보여 준다 (Android `InterstitialAdController` 대응)
///
/// 보여 줄 때 광고를 받으려고 기다리게 하지 않도록 미리 받아 둔다. 보여 줄 차례인지는 정하지 않는다.
/// 그 판단은 `MainViewModel.shouldShowInterstitialAd()`가 맡는다.
@MainActor
final class InterstitialAdPresenter: NSObject {
    private let adUnitID: String
    private var interstitialAd: InterstitialAd?
    private var isLoading = false
    private var isPresenting = false
    private var onShown: (() -> Void)?
    private let logger = Logger(subsystem: "com.deokwooahn.calcmoney", category: "InterstitialAd")

    init(adUnitID: String) {
        self.adUnitID = adUnitID
    }

    var isReady: Bool {
        interstitialAd != nil && !isPresenting
    }

    /// 받아 둔 광고가 없으면 하나 받아 둔다. 광고 동의가 끝나 광고를 요청할 수 있을 때만 부른다.
    func preload() {
        guard interstitialAd == nil, !isLoading, !isPresenting else { return }

        isLoading = true
        Task {
            defer { isLoading = false }

            do {
                let ad = try await InterstitialAd.load(with: adUnitID, request: Request())
                ad.fullScreenContentDelegate = self
                interstitialAd = ad
            } catch {
                logger.warning("Failed to load interstitial ad: \(error.localizedDescription)")
            }
        }
    }

    /// 받아 둔 광고를 보여 준다. 광고가 화면에 뜨면 `onShown`을 부르고, 닫히면 다음 광고를 받아 둔다.
    ///
    /// 받아 둔 광고가 없거나, 앱이 화면 앞에 있지 않거나, 띄울 화면이 없으면 아무것도 하지 않는다.
    func present(onShown: @escaping () -> Void) {
        guard let interstitialAd, !isPresenting else { return }
        guard UIApplication.shared.applicationState == .active else { return }
        guard let rootViewController = Self.topViewController() else {
            logger.warning("No view controller available; interstitial ad was not presented")
            return
        }

        self.interstitialAd = nil
        self.onShown = onShown
        isPresenting = true
        interstitialAd.present(from: rootViewController)
    }

    private func finishPresenting() {
        isPresenting = false
        onShown = nil
        preload()
    }

    /// 시트처럼 이미 올라온 화면이 있으면 그 위에 띄워야 하므로 가장 위의 화면을 찾는다.
    private static func topViewController() -> UIViewController? {
        let rootViewController = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController

        var top = rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }

        return top
    }
}

extension InterstitialAdPresenter: FullScreenContentDelegate {
    func adWillPresentFullScreenContent(_: any FullScreenPresentingAd) {
        onShown?()
    }

    func adDidDismissFullScreenContent(_: any FullScreenPresentingAd) {
        finishPresenting()
    }

    func ad(_: any FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: any Error) {
        logger.warning("Failed to present interstitial ad: \(error.localizedDescription)")
        finishPresenting()
    }
}
