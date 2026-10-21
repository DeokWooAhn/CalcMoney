import Domain

/// 즐겨찾기 구간을 가리키는 빠른 이동 인덱스 라벨 (Android `FAVORITE_INDEX_LABEL` 대응)
let favoriteIndexLabel = "★"

/// 알파벳으로 시작하지 않는 통화 코드를 모으는 인덱스 라벨 (Android `OTHER_INDEX_LABEL` 대응)
let otherIndexLabel = "#"

/// 통화 선택 목록에 보여줄 순서와 빠른 이동 인덱스 (Android `CurrencyPickerList` 대응)
struct CurrencyPickerList: Equatable {
    struct IndexEntry: Equatable {
        let label: String
        /// 이 구간에서 가장 위에 있는 통화 코드. 목록 행의 id라 바로 스크롤 대상으로 쓴다.
        let firstCode: String
    }

    /// 화면에 보여줄 순서대로 정렬한 통화 목록
    let currencies: [CurrencyInfo]
    /// 위에서부터 보여줄 인덱스 라벨
    let indexEntries: [IndexEntry]

    /// 즐겨찾기를 맨 위에 두고 나머지를 통화 코드 알파벳순으로 정렬한다.
    ///
    /// 서버가 주는 순서에 기대지 않고 여기서 정렬해야 인덱스의 글자 구간이 흩어지지 않는다.
    init(currencies: [CurrencyInfo], favoriteCodesForSort: [String]) {
        var favoriteOrder: [String: Int] = [:]
        for (offset, code) in favoriteCodesForSort.enumerated() where favoriteOrder[code] == nil {
            favoriteOrder[code] = offset
        }

        let favorites = currencies
            .filter { favoriteOrder[$0.code] != nil }
            .sorted { (favoriteOrder[$0.code] ?? .max) < (favoriteOrder[$1.code] ?? .max) }
        // 알파벳으로 시작하지 않는 코드는 `#` 구간으로 묶어 맨 뒤에 둔다.
        let others = currencies
            .filter { favoriteOrder[$0.code] == nil }
            .sorted { lhs, rhs in
                let lhsIsOther = Self.indexLabel(of: lhs.code) == otherIndexLabel
                let rhsIsOther = Self.indexLabel(of: rhs.code) == otherIndexLabel
                if lhsIsOther != rhsIsOther { return rhsIsOther }

                return lhs.code.uppercased() < rhs.code.uppercased()
            }

        var entries: [IndexEntry] = []
        if let firstFavorite = favorites.first {
            entries.append(IndexEntry(label: favoriteIndexLabel, firstCode: firstFavorite.code))
        }
        var seenLabels = Set<String>()
        for currency in others {
            let label = Self.indexLabel(of: currency.code)
            if seenLabels.insert(label).inserted {
                entries.append(IndexEntry(label: label, firstCode: currency.code))
            }
        }

        self.currencies = favorites + others
        indexEntries = entries
    }

    /// 검색 결과 목록. 검색 결과에는 빠른 이동 인덱스가 없다.
    init(searchResults: [CurrencyInfo]) {
        currencies = searchResults
        indexEntries = []
    }

    /// 검색어에 맞는 통화를 찾는다 (Android `searchCurrencies` 대응)
    ///
    /// 통화 코드가 같은 것 → 코드가 검색어로 시작하는 것 → 이름이 검색어로 시작하는 것 → 이름에 검색어가 들어간 것
    /// 순으로 둔다. 같은 순위에서는 즐겨찾기를 먼저, 그다음 통화 코드 알파벳순으로 둔다.
    /// 대소문자와 공백은 무시해서 "미국달러"로도 "미국 달러"를 찾는다.
    ///
    /// - Parameters:
    ///   - query: 검색어. 비어 있으면 빈 배열을 돌려준다.
    ///   - favoriteCodesForSort: 같은 순위 안에서 먼저 보여줄 즐겨찾기 코드. 앞에 있을수록 위에 온다.
    ///   - namesOf: 통화 하나에서 검색할 이름들. 화면 언어 이름, 서버가 준 한국어 이름, 영어 이름을 넘긴다.
    static func search(
        _ currencies: [CurrencyInfo],
        query: String,
        favoriteCodesForSort: [String],
        namesOf: (CurrencyInfo) -> [String],
    ) -> [CurrencyInfo] {
        let normalizedQuery = normalizedForSearch(query)
        guard !normalizedQuery.isEmpty else { return [] }

        var favoriteOrder: [String: Int] = [:]
        for (offset, code) in favoriteCodesForSort.enumerated() where favoriteOrder[code] == nil {
            favoriteOrder[code] = offset
        }

        return currencies
            .compactMap { currency in
                match(currency, normalizedQuery: normalizedQuery, names: namesOf(currency)).map { (currency, $0) }
            }
            .sorted { lhs, rhs in
                if lhs.1 != rhs.1 { return lhs.1 < rhs.1 }

                let lhsFavorite = favoriteOrder[lhs.0.code] ?? .max
                let rhsFavorite = favoriteOrder[rhs.0.code] ?? .max
                if lhsFavorite != rhsFavorite { return lhsFavorite < rhsFavorite }

                return lhs.0.code.uppercased() < rhs.0.code.uppercased()
            }
            .map(\.0)
    }

    /// 검색어가 통화와 맞는 정도. 앞에 있을수록 먼저 보여준다.
    private enum SearchMatch: Int, Comparable {
        case exactCode
        case codePrefix
        case namePrefix
        case nameContains

        static func < (lhs: Self, rhs: Self) -> Bool {
            lhs.rawValue < rhs.rawValue
        }
    }

    private static func match(_ currency: CurrencyInfo, normalizedQuery: String, names: [String]) -> SearchMatch? {
        let codes = [currency.code, currency.displayCode].map(normalizedForSearch)
        let normalizedNames = names.map(normalizedForSearch)

        if codes.contains(normalizedQuery) { return .exactCode }
        if codes.contains(where: { $0.hasPrefix(normalizedQuery) }) { return .codePrefix }
        if normalizedNames.contains(where: { $0.hasPrefix(normalizedQuery) }) { return .namePrefix }
        if normalizedNames.contains(where: { $0.contains(normalizedQuery) }) { return .nameContains }

        return nil
    }

    private static func normalizedForSearch(_ text: String) -> String {
        text.filter { !$0.isWhitespace }.lowercased()
    }

    private static func indexLabel(of code: String) -> String {
        guard let first = code.uppercased().first, first.isASCII, first.isLetter else { return otherIndexLabel }

        return String(first)
    }
}
