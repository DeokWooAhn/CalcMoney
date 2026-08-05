import Domain
import FirebaseFirestore
import Foundation

/// Firestore에서 최신 환율 문서를 읽는 원격 데이터소스 (Android `ExchangeRateRemoteDataSource` 대응)
///
/// 앱은 한국수출입은행 API를 직접 호출하지 않고, Cloud Functions가 갱신한
/// `exchangeRates/latest` 문서를 읽는다.
public final class FirestoreExchangeRateDataSource: ExchangeRateRemoteDataSource {
    private static let exchangeRatesCollection = "exchangeRates"
    private static let latestDocumentID = "latest"

    public init() {}

    public func fetchExchangeRates() async throws -> [ExchangeRateData] {
        let snapshot: DocumentSnapshot
        do {
            snapshot = try await Firestore.firestore()
                .collection(Self.exchangeRatesCollection)
                .document(Self.latestDocumentID)
                .getDocument()
        } catch {
            throw Self.mapFirestoreError(error)
        }

        let document = ExchangeRateRemoteDocument(
            exists: snapshot.exists,
            rateDate: snapshot.get("rateDate") as? String,
            rateFetchedAt: Self.intValue(snapshot.get("rateFetchedAt")),
            fetchedAt: Self.intValue(snapshot.get("fetchedAt")),
            rates: snapshot.get("rates"),
            lastError: snapshot.get("lastError") as? String,
        )

        return try ExchangeRateDocumentParser.parse(document)
    }

    private static func intValue(_ value: Any?) -> Int? {
        (value as? NSNumber)?.intValue
    }

    private static func mapFirestoreError(_ error: any Error) -> ExchangeRateError {
        let nsError = error as NSError

        guard nsError.domain == FirestoreErrorDomain else {
            return .temporarilyUnavailable(cause: error)
        }

        return switch FirestoreErrorCode.Code(rawValue: nsError.code) {
        case .unavailable, .deadlineExceeded:
            .networkUnavailable(cause: error)

        default:
            .temporarilyUnavailable(cause: error)
        }
    }
}
