import Presentation
import SwiftUI

@main
struct CalcMoneyApp: App {
    private let container = AppContainer()

    var body: some Scene {
        WindowGroup {
            MainTabView(
                calculatorViewModel: container.calculatorViewModel,
                exchangeViewModel: container.exchangeViewModel,
                mainViewModel: container.mainViewModel,
                adConsentManager: container.adConsentManager,
            )
        }
    }
}
