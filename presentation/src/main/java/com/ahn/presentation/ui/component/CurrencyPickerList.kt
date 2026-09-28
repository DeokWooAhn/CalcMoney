package com.ahn.presentation.ui.component

import com.ahn.domain.currency.model.CurrencyInfo

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

private fun indexLabelOf(code: String): String {
    val first = code.firstOrNull()?.uppercaseChar() ?: return OTHER_INDEX_LABEL

    return if (first in 'A'..'Z') first.toString() else OTHER_INDEX_LABEL
}
