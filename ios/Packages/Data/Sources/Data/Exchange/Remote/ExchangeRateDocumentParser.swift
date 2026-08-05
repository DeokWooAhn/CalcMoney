import Domain
import Foundation

/// Firestore `exchangeRates/latest` 문서의 필드 값 (파싱 로직 테스트용으로 분리)
struct ExchangeRateRemoteDocument {
    let exists: Bool
    let rateDate: String?
    let rateFetchedAt: Int?
    let fetchedAt: Int?
    let rates: Any?
    let lastError: String?
}

/// Firestore 문서를 환율 목록으로 변환하는 파서 (Android 원격 매퍼 + 예외 분기 대응)
enum ExchangeRateDocumentParser {
    private static let unknownCurrencyName = "Unknown"
    /// Cloud Functions는 상류 API의 데이터 없음 상태를 lastError 텍스트로 저장한다.
    private static let notReadyErrorPatterns = ["아직 고시", "no data"]

    static func parse(_ document: ExchangeRateRemoteDocument) throws -> [ExchangeRateData] {
        guard document.exists else {
            throw ExchangeRateError.notReady()
        }

        guard let fetchedAt = document.rateFetchedAt ?? document.fetchedAt else {
            throw ExchangeRateError.temporarilyUnavailable()
        }

        let entities = mapRates(
            document.rates,
            fetchedAt: fetchedAt,
            rateDate: document.rateDate ?? "",
        )

        guard !entities.isEmpty else {
            throw emptyRatesError(lastError: document.lastError ?? "")
        }

        return entities
    }

    private static func mapRates(_ rates: Any?, fetchedAt: Int, rateDate: String) -> [ExchangeRateData] {
        guard let rates = rates as? [Any] else { return [] }

        return rates.compactMap { mapRate($0, fetchedAt: fetchedAt, rateDate: rateDate) }
    }

    private static func mapRate(_ rate: Any, fetchedAt: Int, rateDate: String) -> ExchangeRateData? {
        guard let rate = rate as? [String: Any] else { return nil }
        guard let code = rate["code"] as? String else { return nil }
        guard let baseRate = doubleValue(rate["baseRate"]) else { return nil }

        return ExchangeRateData(
            code: code,
            currencyUnit: rate["currencyUnit"] as? String ?? code,
            currencyName: rate["currencyName"] as? String ?? unknownCurrencyName,
            baseRate: baseRate,
            fetchedAt: fetchedAt,
            rateDate: rateDate,
        )
    }

    private static func doubleValue(_ value: Any?) -> Double? {
        switch value {
        case let number as NSNumber:
            number.doubleValue

        case let string as String:
            Double(string.replacingOccurrences(of: ",", with: ""))

        default:
            nil
        }
    }

    private static func emptyRatesError(lastError: String) -> ExchangeRateError {
        let normalized = lastError.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()

        return if notReadyErrorPatterns.contains(where: { normalized.contains($0) }) {
            .notReady()
        } else {
            .temporarilyUnavailable()
        }
    }
}
