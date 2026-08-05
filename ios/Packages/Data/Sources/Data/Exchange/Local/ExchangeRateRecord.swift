import SwiftData

/// 환율 캐시 SwiftData 레코드 (Android Room `exchange_rates` 테이블 대응)
@Model
final class ExchangeRateRecord {
    @Attribute(.unique) var code: String
    var currencyUnit: String
    var currencyName: String
    var baseRate: Double
    var fetchedAt: Int
    var rateDate: String
    /// 원격 응답의 순서 보존용 (Room rowid 순서 대응 — SwiftData는 삽입 순서를 보장하지 않는다)
    var sortOrder: Int

    init(_ data: ExchangeRateData, sortOrder: Int) {
        code = data.code
        currencyUnit = data.currencyUnit
        currencyName = data.currencyName
        baseRate = data.baseRate
        fetchedAt = data.fetchedAt
        rateDate = data.rateDate
        self.sortOrder = sortOrder
    }

    var asData: ExchangeRateData {
        ExchangeRateData(
            code: code,
            currencyUnit: currencyUnit,
            currencyName: currencyName,
            baseRate: baseRate,
            fetchedAt: fetchedAt,
            rateDate: rateDate,
        )
    }
}
