import Domain
import Testing
@testable import Presentation

@Suite("통화 선택 목록 정렬과 인덱스")
struct CurrencyPickerListTests {
    /// 서버가 알파벳순이 아닌 순서로 줘도 화면에서는 정렬돼야 한다.
    private let currencies = ["USD", "KRW", "AED", "JPY", "EUR", "AUD", "JEP"].map(currency)

    @Test("즐겨찾기가 없으면 통화 코드 알파벳순으로 정렬한다")
    func 즐겨찾기가_없으면_알파벳순() {
        let pickerList = CurrencyPickerList(currencies: currencies, favoriteCodesForSort: [])

        #expect(pickerList.currencies.map(\.code) == ["AED", "AUD", "EUR", "JEP", "JPY", "KRW", "USD"])
    }

    @Test("각 글자 구간의 첫 통화를 인덱스로 가진다")
    func 글자_구간의_첫_통화를_인덱스로_가진다() {
        let pickerList = CurrencyPickerList(currencies: currencies, favoriteCodesForSort: [])

        #expect(pickerList.indexEntries.map(\.label) == ["A", "E", "J", "K", "U"])
        #expect(pickerList.indexEntries.map(\.firstCode) == ["AED", "EUR", "JEP", "KRW", "USD"])
    }

    @Test("즐겨찾기를 등록 순서대로 맨 위에 두고 나머지는 알파벳순으로 잇는다")
    func 즐겨찾기가_맨_위에_온다() {
        let pickerList = CurrencyPickerList(currencies: currencies, favoriteCodesForSort: ["USD", "JPY"])

        #expect(pickerList.currencies.map(\.code) == ["USD", "JPY", "AED", "AUD", "EUR", "JEP", "KRW"])
        #expect(pickerList.indexEntries.first == .init(label: favoriteIndexLabel, firstCode: "USD"))
        #expect(pickerList.indexEntries.map(\.label) == [favoriteIndexLabel, "A", "E", "J", "K"])
    }

    @Test("즐겨찾기 코드가 중복되거나 목록에 없는 코드가 섞여도 처음 등록한 순서를 따른다")
    func 중복되거나_없는_즐겨찾기는_무시한다() {
        let pickerList = CurrencyPickerList(
            currencies: currencies,
            favoriteCodesForSort: ["EUR", "ZZZ", "EUR", "AED"],
        )

        #expect(pickerList.currencies.prefix(2).map(\.code) == ["EUR", "AED"])
        #expect(pickerList.currencies.count == currencies.count)
    }

    @Test("대소문자와 관계없이 정렬하고 알파벳이 아닌 코드는 # 구간으로 맨 뒤에 둔다")
    func 알파벳이_아닌_코드는_맨_뒤() {
        let pickerList = CurrencyPickerList(
            currencies: ["USD", "1AB", "aud"].map(currency),
            favoriteCodesForSort: [],
        )

        #expect(pickerList.currencies.map(\.code) == ["aud", "USD", "1AB"])
        #expect(pickerList.indexEntries.map(\.label) == ["A", "U", otherIndexLabel])
    }
}

/// 패키지 기본 격리가 MainActor라, 격리되지 않은 프로퍼티 초기화에서 함수 참조로 넘기려면 `nonisolated`가 필요하다.
private nonisolated func currency(_ code: String) -> CurrencyInfo {
    CurrencyInfo(code: code, displayCode: code, name: "\(code) 통화", flagEmoji: "🌐")
}
