package com.ahn.presentation.ui.component

import com.ahn.domain.currency.model.CurrencyInfo
import java.util.Locale

/** 즐겨찾기 구간을 가리키는 빠른 이동 인덱스 라벨입니다. */
internal const val FAVORITE_INDEX_LABEL = "★"

/** 알파벳으로 시작하지 않는 통화 코드를 모으는 인덱스 라벨입니다. */
internal const val OTHER_INDEX_LABEL = "#"

/**
 * 통화 선택 목록에 보여줄 순서와 빠른 이동 인덱스입니다.
 *
 * @property currencies 화면에 보여줄 순서대로 정렬한 통화 목록입니다.
 * @property indexPositions 인덱스 라벨과 그 구간이 [currencies]에서 시작하는 위치입니다. 라벨은 보여줄 순서를 유지합니다.
 */
internal data class CurrencyPickerList(
    val currencies: List<CurrencyInfo>,
    val indexPositions: Map<String, Int>,
)

/**
 * 즐겨찾기를 맨 위에 두고 나머지를 통화 코드 알파벳순으로 정렬합니다.
 *
 * 서버가 주는 순서에 기대지 않고 여기서 정렬해야 인덱스의 글자 구간이 흩어지지 않습니다.
 *
 * @param currencies 선택할 수 있는 통화 목록입니다.
 * @param favoriteCodesForSort 즐겨찾기 구간의 정렬 기준입니다. 앞에 있을수록 위에 옵니다.
 */
internal fun buildCurrencyPickerList(
    currencies: List<CurrencyInfo>,
    favoriteCodesForSort: List<String>,
): CurrencyPickerList {
    val favoriteOrder = favoriteCodesForSort.distinct().withIndex().associate { it.value to it.index }
    val favorites = currencies
        .filter { it.code in favoriteOrder }
        .sortedBy { favoriteOrder[it.code] }
    // 알파벳으로 시작하지 않는 코드는 `#` 구간으로 묶어 맨 뒤에 둔다.
    val others = currencies
        .filter { it.code !in favoriteOrder }
        .sortedWith(compareBy({ indexLabelOf(it.code) == OTHER_INDEX_LABEL }, { it.code.uppercase() }))

    val indexPositions = linkedMapOf<String, Int>()
    if (favorites.isNotEmpty()) indexPositions[FAVORITE_INDEX_LABEL] = 0
    others.forEachIndexed { index, currency ->
        indexPositions.putIfAbsent(indexLabelOf(currency.code), favorites.size + index)
    }

    return CurrencyPickerList(currencies = favorites + others, indexPositions = indexPositions)
}

/**
 * 검색어에 맞는 통화를 찾습니다.
 *
 * 통화 코드가 같은 것 → 코드가 검색어로 시작하는 것 → 이름이 검색어로 시작하는 것 → 이름에 검색어가 들어간 것
 * 순으로 둡니다. 같은 순위에서는 즐겨찾기를 먼저, 그다음 통화 코드 알파벳순으로 둡니다.
 * 대소문자와 공백은 무시해서 "미국달러"로도 "미국 달러"를 찾습니다.
 *
 * @param query 검색어입니다. 비어 있으면 빈 목록을 돌려주므로, 검색 중이 아닐 때는 [buildCurrencyPickerList]를 씁니다.
 * @param favoriteCodesForSort 같은 순위 안에서 먼저 보여줄 즐겨찾기 코드입니다. 앞에 있을수록 위에 옵니다.
 * @param namesOf 통화 하나에서 검색할 이름들입니다. 화면 언어 이름, 서버가 준 한국어 이름, 영어 이름을 넘깁니다.
 */
internal fun searchCurrencies(
    currencies: List<CurrencyInfo>,
    query: String,
    favoriteCodesForSort: List<String>,
    namesOf: (CurrencyInfo) -> List<String>,
): List<CurrencyInfo> {
    val normalizedQuery = query.normalizedForSearch()
    if (normalizedQuery.isEmpty()) return emptyList()

    val favoriteOrder = favoriteCodesForSort.distinct().withIndex().associate { it.value to it.index }

    return currencies
        .mapNotNull { currency ->
            matchOf(currency, normalizedQuery, namesOf(currency))?.let { match -> currency to match }
        }
        .sortedWith(
            compareBy(
                { (_, match) -> match.ordinal },
                { (currency, _) -> favoriteOrder[currency.code] ?: Int.MAX_VALUE },
                { (currency, _) -> currency.code.uppercase() },
            ),
        )
        .map { (currency, _) -> currency }
}

/** 검색어가 통화와 맞는 정도입니다. 앞에 있을수록 먼저 보여줍니다. */
private enum class SearchMatch { EXACT_CODE, CODE_PREFIX, NAME_PREFIX, NAME_CONTAINS }

private fun matchOf(currency: CurrencyInfo, normalizedQuery: String, names: List<String>): SearchMatch? {
    val codes = listOf(currency.code, currency.displayCode).map { it.normalizedForSearch() }
    val normalizedNames = names.map { it.normalizedForSearch() }

    return when {
        codes.any { it == normalizedQuery } -> SearchMatch.EXACT_CODE
        codes.any { it.startsWith(normalizedQuery) } -> SearchMatch.CODE_PREFIX
        normalizedNames.any { it.startsWith(normalizedQuery) } -> SearchMatch.NAME_PREFIX
        normalizedNames.any { normalizedQuery in it } -> SearchMatch.NAME_CONTAINS
        else -> null
    }
}

private fun String.normalizedForSearch(): String = filterNot { it.isWhitespace() }.lowercase(Locale.ROOT)

private fun indexLabelOf(code: String): String {
    val first = code.firstOrNull()?.uppercaseChar() ?: return OTHER_INDEX_LABEL

    return if (first in 'A'..'Z') first.toString() else OTHER_INDEX_LABEL
}
