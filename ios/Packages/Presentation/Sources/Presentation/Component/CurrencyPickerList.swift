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

    private static func indexLabel(of code: String) -> String {
        guard let first = code.uppercased().first, first.isASCII, first.isLetter else { return otherIndexLabel }

        return String(first)
    }
}
