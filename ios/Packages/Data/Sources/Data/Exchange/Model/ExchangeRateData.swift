/// 환율 데이터 전달용 값 타입 (Android `ExchangeRateEntity` 대응)
///
/// SwiftData 레코드와 원격 응답 사이를 오가는 순수 값이다.
public struct ExchangeRateData: Equatable, Sendable {
    public let code: String
    public let currencyUnit: String
    public let currencyName: String
    public let baseRate: Double
    public let fetchedAt: Int
    public let rateDate: String

    public init(
        code: String,
        currencyUnit: String,
        currencyName: String,
        baseRate: Double,
        fetchedAt: Int,
        rateDate: String,
    ) {
        self.code = code
        self.currencyUnit = currencyUnit
        self.currencyName = currencyName
        self.baseRate = baseRate
        self.fetchedAt = fetchedAt
        self.rateDate = rateDate
    }
}
