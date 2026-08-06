import GoogleMobileAds
import Observation
import OSLog
import UIKit
import UserMessagingPlatform

/// UMP 동의 흐름을 관리한다 (Android `AdConsentManager` 대응).
///
/// Google 정책에 따라 앱 시작 시마다 동의 상태를 갱신하고,
/// 필요하면 동의 양식을 띄우며, 개인정보 옵션 진입점 노출 여부를 제공한다.
@MainActor
@Observable
public final class AdConsentManager {
    public private(set) var canRequestAds = false
    public private(set) var isPrivacyOptionsRequired = false

    @ObservationIgnored private var isMobileAdsStarted = false
    @ObservationIgnored private let logger = Logger(subsystem: "com.ahn.CalcMoney", category: "AdConsent")

    public init() {}

    /// UI 테스트가 넘기는 실행 인자. `CalcMoneyUITests`의 `launchApp()`과 값이 같아야 한다.
    ///
    /// UserDefaults가 하이픈으로 시작하는 인자를 키-값 쌍으로 해석해 뒤따르는 인자를 삼키므로
    /// (`-AppleLanguages (ko)`처럼) 여기서는 하이픈을 붙이지 않는다.
    static let uiTestingLaunchArgument = "UI_TESTING"

    private static var isUITesting: Bool {
        ProcessInfo.processInfo.arguments.contains(uiTestingLaunchArgument)
    }

    /// 앱 시작 시 호출: 동의 상태 갱신 → 필요 시 동의 양식 표시 → 광고 SDK 시작.
    public func gatherConsent() async {
        // UI 테스트가 UMP의 지역 판정에 좌우되면 안 된다. EEA로 판정되면 동의 양식이 화면을
        // 덮어 테스트가 통째로 막히고, 그 전에도 실행마다 네트워크 왕복이 붙는다.
        // 여기서 빠져나가면 canRequestAds가 false로 남아 배너 로드까지 함께 건너뛴다.
        if Self.isUITesting {
            logger.info("UI testing detected; skipping ad consent flow")

            return
        }

        do {
            try await requestConsentInfoUpdate()

            if let rootViewController = await Self.awaitRootViewController() {
                try await loadAndPresentConsentFormIfRequired(from: rootViewController)
            } else {
                // 여기서 조용히 넘어가면 동의 양식이 필요한 사용자에게 양식을 못 띄운 채
                // 세션 내내 광고가 뜨지 않는데, 원인을 남기는 흔적이 하나도 없다.
                logger.warning("No root view controller available; ad consent form was not presented")
            }
        } catch {
            logger.warning("Failed to gather ad consent: \(error.localizedDescription)")
        }

        updateConsentState()
        startMobileAdsIfNeeded()
    }

    /// 설정 화면의 개인정보 옵션 진입점에서 호출한다.
    public func presentPrivacyOptionsForm() async {
        guard let rootViewController = Self.rootViewController() else {
            // 사용자가 설정에서 직접 누른 시점이라 창이 없을 가능성은 낮지만,
            // 그래도 없으면 아무 일도 일어나지 않으므로 흔적을 남긴다.
            logger.warning("No root view controller available; privacy options form was not presented")

            return
        }

        do {
            try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, any Error>) in
                ConsentForm.presentPrivacyOptionsForm(from: rootViewController) { error in
                    if let error {
                        continuation.resume(throwing: error)
                    } else {
                        continuation.resume()
                    }
                }
            }
        } catch {
            logger.warning("Failed to present privacy options form: \(error.localizedDescription)")
        }

        updateConsentState()
    }

    private func requestConsentInfoUpdate() async throws {
        let parameters = RequestParameters()

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, any Error>) in
            ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume()
                }
            }
        }
    }

    private func loadAndPresentConsentFormIfRequired(from viewController: UIViewController) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, any Error>) in
            ConsentForm.loadAndPresentIfRequired(from: viewController) { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume()
                }
            }
        }
    }

    private func updateConsentState() {
        canRequestAds = ConsentInformation.shared.canRequestAds
        isPrivacyOptionsRequired =
            ConsentInformation.shared.privacyOptionsRequirementStatus == .required
    }

    private func startMobileAdsIfNeeded() {
        guard canRequestAds, !isMobileAdsStarted else { return }

        isMobileAdsStarted = true
        MobileAds.shared.start()
    }

    /// 키 윈도가 아직 준비되지 않았을 수 있어 잠깐 기다리며 다시 확인한다.
    ///
    /// `gatherConsent()`는 뷰의 `.task`에서 도는데, 콜드 스타트 시 창이 key가 되기 전에
    /// 여기까지 올 수 있다. 최대 1초만 기다리고 그래도 없으면 nil을 돌려준다.
    private static func awaitRootViewController(
        retryCount: Int = 5,
        retryInterval: Duration = .milliseconds(200),
    ) async -> UIViewController? {
        for _ in 0..<retryCount {
            if let rootViewController = rootViewController() {
                return rootViewController
            }

            try? await Task.sleep(for: retryInterval)
        }

        return rootViewController()
    }

    private static func rootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    }
}
