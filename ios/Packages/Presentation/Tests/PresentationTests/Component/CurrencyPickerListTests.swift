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

@Suite("통화 검색")
struct CurrencySearchTests {
    /// 화면 언어 이름과 영어 이름을 함께 검색한다.
    private let names: [String: [String]] = [
        "USD": ["미국 달러", "US Dollar"],
        "AUD": ["호주 달러", "Australian Dollar"],
        "CAD": ["캐나다 달러", "Canadian Dollar"],
        "JPY": ["일본 엔", "Japanese Yen"],
        "SAR": ["사우디아라비아 리얄", "Saudi Riyal"],
        "VND": ["베트남 동", "Vietnamese Dong"],
    ]

    private func search(_ query: String, favorites: [String] = []) -> [String] {
        CurrencyPickerList.search(
            names.keys.sorted().map(currency),
            query: query,
            favoriteCodesForSort: favorites,
            namesOf: { names[$0.code] ?? [] },
        )
        .map(\.code)
    }

    @Test("나라 이름으로 찾으면 이름에 그 나라가 들어간 통화를 찾는다")
    func 나라_이름으로_찾는다() {
        #expect(search("베트남") == ["VND"])
    }

    @Test("통화 코드는 대소문자와 관계없이 찾는다")
    func 코드는_대소문자와_관계없이_찾는다() {
        #expect(search("jpy") == ["JPY"])
    }

    @Test("코드가 같은 것, 코드로 시작하는 것, 이름으로 시작하는 것, 이름에 들어간 것 순으로 둔다")
    func 맞는_정도_순으로_둔다() {
        // 알파벳순(ABC, ABCD, QQQ, XYZ)과 다르게 나와야 순위대로 정렬한 것이다.
        let rankedNames = ["ABC": ["Alpha"], "ABCD": ["Beta"], "XYZ": ["Abc Dollar"], "QQQ": ["My Abc"]]
        let result = CurrencyPickerList.search(
            ["QQQ", "XYZ", "ABCD", "ABC"].map(currency),
            query: "abc",
            favoriteCodesForSort: [],
            namesOf: { rankedNames[$0.code] ?? [] },
        )

        #expect(result.map(\.code) == ["ABC", "ABCD", "XYZ", "QQQ"])
    }

    @Test("같은 순위에서는 즐겨찾기를 먼저 두고 나머지는 통화 코드 알파벳순으로 둔다")
    func 같은_순위에서는_즐겨찾기가_먼저다() {
        #expect(search("달러") == ["AUD", "CAD", "USD"])
        #expect(search("달러", favorites: ["USD"]) == ["USD", "AUD", "CAD"])
    }

    @Test("공백을 무시하고 이름을 찾는다")
    func 공백을_무시한다() {
        #expect(search("미국달러") == ["USD"])
    }

    @Test("검색어가 비어 있거나 맞는 통화가 없으면 빈 배열이다")
    func 결과가_없으면_빈_배열() {
        #expect(search("  ").isEmpty)
        #expect(search("없는통화").isEmpty)
    }

    @Test("검색 결과 목록에는 빠른 이동 인덱스가 없다")
    func 검색_결과에는_인덱스가_없다() {
        let pickerList = CurrencyPickerList(searchResults: ["USD"].map(currency))

        #expect(pickerList.currencies.map(\.code) == ["USD"])
        #expect(pickerList.indexEntries.isEmpty)
    }
}

/// 패키지 기본 격리가 MainActor라, 격리되지 않은 프로퍼티 초기화에서 함수 참조로 넘기려면 `nonisolated`가 필요하다.
private nonisolated func currency(_ code: String) -> CurrencyInfo {
    CurrencyInfo(code: code, displayCode: code, name: "\(code) 통화", flagEmoji: "🌐")
}
