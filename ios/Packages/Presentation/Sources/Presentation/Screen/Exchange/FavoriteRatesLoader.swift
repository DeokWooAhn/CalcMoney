import Domain
import Foundation

/// 즐겨찾기 카드를 만드는 데 필요한 환율 상태. 이 값이 바뀔 때만 환율을 다시 모은다 (Android `FavoriteRateInput` 대응).
///
/// 금액(`fromAmount`)은 들어 있지 않다. 금액만 바뀌면 이미 모은 환율로 카드만 다시 만든다.
/// 새로고침이 성공하기 전까지는 이 값이 바뀌지 않아야 즐겨찾기 화면의 실패 분기가 끝까지 보인다.
struct FavoriteRateInput: Equatable {
    let fromCurrency: CurrencyInfo?
    let favoriteCurrencyCodes: [String]
    let availableCurrencies: [CurrencyInfo]
    let exchangeRateDate: String
    let exchangeRateFetchedAt: Int
    /// 같은 서버 문서를 다시 받아도 다시 시도가 카드를 다시 모으게 한다.
    let ratesRefreshCount: Int

    init(
        fromCurrency: CurrencyInfo?,
        favoriteCurrencyCodes: [String],
        availableCurrencies: [CurrencyInfo],
        exchangeRateDate: String = "",
        exchangeRateFetchedAt: Int = 0,
        ratesRefreshCount: Int = 0,
    ) {
        self.fromCurrency = fromCurrency
        self.favoriteCurrencyCodes = favoriteCurrencyCodes
        self.availableCurrencies = availableCurrencies
        self.exchangeRateDate = exchangeRateDate
        self.exchangeRateFetchedAt = exchangeRateFetchedAt
        self.ratesRefreshCount = ratesRefreshCount
    }

    init(_ state: ExchangeState) {
        self.init(
            fromCurrency: state.fromCurrency,
            favoriteCurrencyCodes: state.favoriteCurrencyCodes,
            availableCurrencies: state.availableCurrencies,
            exchangeRateDate: state.exchangeRateDate,
            exchangeRateFetchedAt: state.exchangeRateFetchedAt,
            ratesRefreshCount: state.ratesRefreshCount,
        )
    }
}

/// 조회를 마친 입력과 그 입력으로 모은 환율. 둘을 늘 함께 쓴다.
///
/// 환율은 `input`의 기준 통화 대비 값이라, 다른 입력과 섞으면 기준 통화가 다른 틀린 카드가 나온다.
struct LoadedFavoriteRates: Equatable {
    let input: FavoriteRateInput
    let ratesByCode: [String: Double]
}

/// 즐겨찾기 통화의 환율을 모으고 카드로 바꾼다. 상태를 들지 않는다 (Android `FavoriteRatesLoader` 대응).
///
/// 언제 다시 모을지는 `ExchangeViewModel`이 정한다.
struct FavoriteRatesLoader {
    let exchangeUseCases: ExchangeUseCases
    let favoriteUseCases: FavoriteUseCases

    /// 환율을 모을 필요가 있는지 판단한다.
    /// 기준 통화는 카드로 만들지 않으므로, 즐겨찾기가 비었거나 기준 통화뿐이면 조회할 환율이 없다.
    func needsRates(_ input: FavoriteRateInput) -> Bool {
        guard let base = input.fromCurrency, !input.availableCurrencies.isEmpty else { return false }

        return input.favoriteCurrencyCodes.contains { $0 != base.code }
    }

    /// 조회할 환율이 없을 때 보여 줄 상태. 즐겨찾기가 기준 통화뿐이면 그 코드를 담는다.
    func emptyRates(_ input: FavoriteRateInput) -> FavoriteRates {
        var rates = FavoriteRates()
        rates.baseOnlyFavoriteCode = baseOnlyFavoriteCode(input)
        return rates
    }

    /// 즐겨찾기 순서대로 기준 통화 대비 환율을 모은다.
    ///
    /// 기준 통화와 통화 목록에 없는 코드는 건너뛰고, 조회에 실패한 코드는 결과에서 뺀다.
    /// 실패 표시는 환율 상태(`ExchangeState.failure`)가 맡는다. 취소되면 더 조회하지 않는다.
    func loadRates(_ input: FavoriteRateInput) async -> [String: Double] {
        guard let base = input.fromCurrency else { return [:] }

        let availableCodes = Set(input.availableCurrencies.map(\.code))
        var rates: [String: Double] = [:]

        for code in input.favoriteCurrencyCodes {
            if Task.isCancelled { break }
            if code == base.code { continue }
            if !availableCodes.contains(code) { continue }

            if let rate = try? await exchangeUseCases.getExchangeRate(from: base.code, to: code) {
                rates[code] = rate
            }
        }

        return rates
    }

    /// 모은 환율과 기준 금액으로 카드를 만든다.
    func buildItems(
        input: FavoriteRateInput,
        baseAmount: String,
        ratesByCode: [String: Double],
    ) -> [FavoriteRateItem] {
        let favoriteRates = favoriteUseCases.buildFavoriteRates(
            baseCurrency: input.fromCurrency,
            baseAmount: baseAmount,
            favoriteCurrencyCodes: input.favoriteCurrencyCodes,
            availableCurrencies: input.availableCurrencies,
            ratesByCode: ratesByCode,
        )

        return favoriteRates.map { rateInfo in
            FavoriteRateItem(
                currency: rateInfo.currency,
                convertedAmount: String(format: "%.2f", rateInfo.convertedAmount),
                rateLabel: "1 \(rateInfo.baseCurrencyCode) = "
                    + String(format: "%.4f", rateInfo.rate)
                    + " \(rateInfo.currency.code)",
            )
        }
    }

    private func baseOnlyFavoriteCode(_ input: FavoriteRateInput) -> String? {
        guard let base = input.fromCurrency, !input.favoriteCurrencyCodes.isEmpty,
              input.favoriteCurrencyCodes.allSatisfy({ $0 == base.code })
        else {
            return nil
        }
        return base.code
    }
}
