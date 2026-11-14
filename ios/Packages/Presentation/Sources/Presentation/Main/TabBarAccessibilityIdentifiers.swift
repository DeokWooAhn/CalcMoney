import SwiftUI
import UIKit

/// SwiftUI 탭 라벨의 식별자가 빠져도 실제 UIKit 탭 항목에는 같은 식별자를 유지한다.
struct TabBarAccessibilityIdentifiers: UIViewControllerRepresentable {
    func makeUIViewController(context _: Context) -> Controller {
        Controller()
    }

    func updateUIViewController(_ controller: Controller, context _: Context) {
        controller.updateIdentifiers()
    }

    final class Controller: UIViewController {
        override func loadView() {
            view = UIView()
            view.isUserInteractionEnabled = false
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            updateIdentifiers()
        }

        override func viewDidLayoutSubviews() {
            super.viewDidLayoutSubviews()
            updateIdentifiers()
        }

        func updateIdentifiers() {
            guard let window = viewIfLoaded?.window else { return }
            installIdentifiers(in: window)
        }

        private func installIdentifiers(in view: UIView) {
            if let tabBar = view as? UITabBar, let items = tabBar.items, items.count == MainTab.allCases.count {
                for (item, tab) in zip(items, MainTab.allCases) where item.accessibilityIdentifier != tab.testID {
                    item.accessibilityIdentifier = tab.testID
                }
                // XCTest는 항목의 식별자를 찾지만 Maestro에는 실제 버튼의 식별자가 필요하다.
                installButtonIdentifiers(in: tabBar)
                return
            }

            for subview in view.subviews {
                installIdentifiers(in: subview)
            }
        }

        private func installButtonIdentifiers(in tabBar: UITabBar) {
            let buttons = controls(in: tabBar)
            guard buttons.count == MainTab.allCases.count else { return }

            let isRightToLeft = tabBar.effectiveUserInterfaceLayoutDirection == .rightToLeft
            let orderedButtons = buttons.sorted { first, second in
                let firstX = first.convert(first.bounds, to: tabBar).midX
                let secondX = second.convert(second.bounds, to: tabBar).midX
                return isRightToLeft ? firstX > secondX : firstX < secondX
            }
            let identifiedButtons = zip(orderedButtons, MainTab.allCases)
            for (button, tab) in identifiedButtons where button.accessibilityIdentifier != tab.testID {
                button.accessibilityIdentifier = tab.testID
            }
        }

        private func controls(in view: UIView) -> [UIControl] {
            if let control = view as? UIControl { return [control] }
            return view.subviews.flatMap { controls(in: $0) }
        }
    }
}
