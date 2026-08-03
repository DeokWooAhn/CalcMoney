public protocol ExchangeRateRepository: Sendable {
    func exchangeRate(from: String, to: String) async throws -> Double

    func latestRateDate() async throws -> String

    func latestFetchedAt() async throws -> Int

    func refreshExchangeRates() async throws

    func supportedCurrencies() async throws -> [CurrencyInfo]
}
