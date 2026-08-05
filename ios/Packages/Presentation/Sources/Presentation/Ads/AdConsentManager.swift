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

    /// 앱 시작 시 호출: 동의 상태 갱신 → 필요 시 동의 양식 표시 → 광고 SDK 시작.
    public func gatherConsent() async {
        do {
            try await requestConsentInfoUpdate()

            if let rootViewController = Self.rootViewController() {
                try await loadAndPresentConsentFormIfRequired(from: rootViewController)
            }
        } catch {
            logger.warning("Failed to gather ad consent: \(error.localizedDescription)")
        }

        updateConsentState()
        startMobileAdsIfNeeded()
    }

    /// 설정 화면의 개인정보 옵션 진입점에서 호출한다.
    public func presentPrivacyOptionsForm() async {
        guard let rootViewController = Self.rootViewController() else { return }

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

    private static func rootViewController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
    }
}
