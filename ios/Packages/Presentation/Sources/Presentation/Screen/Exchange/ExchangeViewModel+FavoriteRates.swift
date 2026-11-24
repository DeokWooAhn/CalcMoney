import Domain
import Foundation

/// 즐겨찾기 카드를 언제 다시 모을지 정한다 (Android `ExchangeViewModel.observeFavoriteRates` 대응).
///
/// 화면은 관여하지 않는다. iOS 17 대상이라 `Observations`를 쓸 수 없어, `state`의 `didSet`이 바뀔 때마다
/// 이 판단을 부른다. 여기서 바꾸는 것은 `favoriteRates`뿐이라 입력·금액이 그대로여서 다시 돌지 않는다.
extension ExchangeViewModel {
    func stateDidChangeForFavoriteRates() {
        let input = FavoriteRateInput(state)
        let amount = state.fromAmount
        let inputChanged = input != observedFavoriteRateInput
        let amountChanged = amount != observedFavoriteBaseAmount

        // 아래에서 카드를 바꾸면 이 함수가 다시 불리므로, 그 전에 본 값을 먼저 기록한다.
        observedFavoriteRateInput = input
        observedFavoriteBaseAmount = amount

        if inputChanged {
            restartFavoriteRates(for: input)
        }
        if amountChanged {
            followFavoriteBaseAmount(amount)
        }
    }

    /// 입력이 바뀌면 진행 중인 조회를 취소하고 새로 모은다.
    private func restartFavoriteRates(for input: FavoriteRateInput) {
        favoriteRatesTask?.cancel()
        favoriteRatesTask = nil

        guard favoriteRatesLoader.needsRates(input) else {
            // 비운 카드가 다음 재조회 중 금액 변경으로 되살아나지 않게 묶음도 비운다.
            loadedFavoriteRates = nil
            let emptyRates = favoriteRatesLoader.emptyRates(input)
            reduceFavoriteRates { _ in emptyRates }
            return
        }

        // 새로고침 중에도 이전 카드는 그대로 둔다.
        reduceFavoriteRates { rates in
            var next = rates
            next.isLoading = true
            next.baseOnlyFavoriteCode = nil
            return next
        }

        let loader = favoriteRatesLoader
        favoriteRatesTask = Task { [weak self] in
            let ratesByCode = await loader.loadRates(input)
            // 취소와 이 확인이 모두 MainActor에서 일어나므로, 뒤 입력으로 취소된 조회는 여기서 멈춘다.
            guard !Task.isCancelled, let self else { return }

            let loaded = LoadedFavoriteRates(input: input, ratesByCode: ratesByCode)
            // 카드를 바꾸기 전에 묶음부터 바꾼다. 카드를 바꾸며 다시 불리는 판단이 옛 묶음을 보지 않게 한다.
            loadedFavoriteRates = loaded
            let items = loader.buildItems(input: input, baseAmount: state.fromAmount, ratesByCode: ratesByCode)
            reduceFavoriteRates { rates in
                var next = rates
                next.isLoading = false
                next.items = items
                return next
            }
            favoriteRatesTask = nil
        }
    }

    /// 금액만 바뀌면 다시 조회하지 않고, 마지막으로 조회를 마친 묶음의 입력·환율로 카드만 다시 만든다.
    ///
    /// 재조회 중이어도 마찬가지다. 새 입력과 옛 환율을 섞지 않는다. 기준 통화가 바뀐 재조회라면
    /// 옛 환율은 옛 기준 통화 대비 값이기 때문이다. 묶음이 없으면(첫 조회 전, 카드를 비운 뒤) 카드를 건드리지 않는다.
    /// 로딩 표시는 건드리지 않는다.
    private func followFavoriteBaseAmount(_ amount: String) {
        guard let loaded = loadedFavoriteRates else { return }

        let items = favoriteRatesLoader.buildItems(
            input: loaded.input,
            baseAmount: amount,
            ratesByCode: loaded.ratesByCode,
        )
        reduceFavoriteRates { rates in
            var next = rates
            next.items = items
            return next
        }
    }
}
