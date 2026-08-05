import Domain
import Testing
@testable import Data

@Suite("ExchangeRateDocumentParser")
struct ExchangeRateDocumentParserTests {
    private func document(
        exists: Bool = true,
        rateDate: String? = "2026-08-04",
        rateFetchedAt: Int? = 1000,
        fetchedAt: Int? = nil,
        rates: Any? = nil,
        lastError: String? = nil,
    ) -> ExchangeRateRemoteDocument {
        ExchangeRateRemoteDocument(
            exists: exists,
            rateDate: rateDate,
            rateFetchedAt: rateFetchedAt,
            fetchedAt: fetchedAt,
            rates: rates,
            lastError: lastError,
        )
    }

    @Test("환율 맵 목록을 엔티티로 변환한다")
    func 환율_맵_목록을_엔티티로_변환한다() throws {
        let rates: [Any] = [
            ["code": "USD", "currencyUnit": "USD", "currencyName": "미국 달러", "baseRate": 1441.1],
            ["code": "JPY", "currencyName": "일본 엔", "baseRate": "9.65"],
        ]

        let result = try ExchangeRateDocumentParser.parse(document(rates: rates))

        #expect(result.count == 2)
        #expect(result[0] == ExchangeRateData(
            code: "USD",
            currencyUnit: "USD",
            currencyName: "미국 달러",
            baseRate: 1441.1,
            fetchedAt: 1000,
            rateDate: "2026-08-04",
        ))
        #expect(result[1].currencyUnit == "JPY")
        #expect(result[1].baseRate == 9.65)
    }

    @Test("쉼표가 든 문자열 환율도 숫자로 변환한다")
    func 쉼표가_든_문자열_환율도_숫자로_변환한다() throws {
        let rates: [Any] = [["code": "KWD", "baseRate": "4,700.5"]]

        let result = try ExchangeRateDocumentParser.parse(document(rates: rates))

        #expect(result[0].baseRate == 4700.5)
        #expect(result[0].currencyName == "Unknown")
    }

    @Test("code나 baseRate가 없는 항목은 건너뛴다")
    func code나_baseRate가_없는_항목은_건너뛴다() throws {
        let rates: [Any] = [
            ["baseRate": 100.0],
            ["code": "USD"],
            ["code": "JPY", "baseRate": 9.65],
            "잘못된 항목",
        ]

        let result = try ExchangeRateDocumentParser.parse(document(rates: rates))

        #expect(result.map(\.code) == ["JPY"])
    }

    @Test("rateFetchedAt이 없으면 fetchedAt으로 대체한다")
    func rateFetchedAt이_없으면_fetchedAt으로_대체한다() throws {
        let rates: [Any] = [["code": "USD", "baseRate": 1441.1]]

        let result = try ExchangeRateDocumentParser.parse(
            document(rateFetchedAt: nil, fetchedAt: 2000, rates: rates),
        )

        #expect(result[0].fetchedAt == 2000)
    }

    @Test("문서가 없으면 notReady 오류를 던진다")
    func 문서가_없으면_notReady_오류를_던진다() {
        #expect(throws: ExchangeRateError.self) {
            try ExchangeRateDocumentParser.parse(document(exists: false))
        }

        do {
            _ = try ExchangeRateDocumentParser.parse(document(exists: false))
        } catch let error as ExchangeRateError {
            guard case .notReady = error else {
                Issue.record("notReady가 아닌 오류: \(error)")
                return
            }
        } catch {
            Issue.record("예상하지 못한 오류: \(error)")
        }
    }

    @Test("fetchedAt 계열 필드가 모두 없으면 temporarilyUnavailable 오류를 던진다")
    func fetchedAt_계열_필드가_모두_없으면_temporarilyUnavailable_오류를_던진다() {
        do {
            _ = try ExchangeRateDocumentParser.parse(document(rateFetchedAt: nil, fetchedAt: nil))
        } catch let error as ExchangeRateError {
            guard case .temporarilyUnavailable = error else {
                Issue.record("temporarilyUnavailable이 아닌 오류: \(error)")
                return
            }
        } catch {
            Issue.record("예상하지 못한 오류: \(error)")
        }
    }

    @Test("환율이 비어 있고 lastError가 데이터 없음 패턴이면 notReady로 분류한다")
    func 빈_환율_lastError_데이터_없음_패턴은_notReady() {
        do {
            _ = try ExchangeRateDocumentParser.parse(
                document(rates: [], lastError: "오늘 환율이 아직 고시되지 않았습니다"),
            )
        } catch let error as ExchangeRateError {
            guard case .notReady = error else {
                Issue.record("notReady가 아닌 오류: \(error)")
                return
            }
        } catch {
            Issue.record("예상하지 못한 오류: \(error)")
        }
    }

    @Test("환율이 비어 있고 lastError가 일반 오류면 temporarilyUnavailable로 분류한다")
    func 빈_환율_일반_오류는_temporarilyUnavailable() {
        do {
            _ = try ExchangeRateDocumentParser.parse(document(rates: [], lastError: "internal error"))
        } catch let error as ExchangeRateError {
            guard case .temporarilyUnavailable = error else {
                Issue.record("temporarilyUnavailable이 아닌 오류: \(error)")
                return
            }
        } catch {
            Issue.record("예상하지 못한 오류: \(error)")
        }
    }
}
