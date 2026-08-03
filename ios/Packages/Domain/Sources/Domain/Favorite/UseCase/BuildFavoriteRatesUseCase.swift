public struct BuildFavoriteRatesUseCase: Sendable {
    public init() {}

    public func callAsFunction(
        baseCurrency: CurrencyInfo?,
        baseAmount: String,
        favoriteCurrencyCodes: [String],
        availableCurrencies: [CurrencyInfo],
        ratesByCode: [String: Double],
    ) -> [FavoriteRateInfo] {
        guard
            let baseCurrency,
            !favoriteCurrencyCodes.isEmpty,
            !availableCurrencies.isEmpty
        else {
            return []
        }

        let baseAmountValue = Double(baseAmount) ?? 0.0
        let currenciesByCode = Dictionary(
            availableCurrencies.map { ($0.code, $0) },
            uniquingKeysWith: { _, last in last },
        )

        var seenCodes = Set<String>()

        return favoriteCurrencyCodes.compactMap { code in
            guard seenCodes.insert(code).inserted else { return nil }
            guard code != baseCurrency.code else { return nil }
            guard let currency = currenciesByCode[code], let rate = ratesByCode[code] else { return nil }

            return FavoriteRateInfo(
                currency: currency,
                baseCurrencyCode: baseCurrency.code,
                rate: rate,
                convertedAmount: baseAmountValue * rate,
            )
        }
    }
}
