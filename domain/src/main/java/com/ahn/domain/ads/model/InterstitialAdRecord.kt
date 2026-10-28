package com.ahn.domain.ads.model

/**
 * 전면 광고를 보여 줄 차례인지 판단하는 데 쓰는 기록입니다.
 *
 * 시각은 epoch milliseconds, 날짜는 기기 시간대 기준 epoch day입니다.
 *
 * @property firstCalculatedAt 계산에 처음 성공한 시각입니다. 이때부터 첫 광고까지의 유예 기간을 셉니다.
 * @property calculationCountSinceLastAd 마지막 광고 뒤로 계산에 성공한 횟수입니다.
 * @property lastShownAt 마지막으로 광고를 보여 준 시각입니다. 보여 준 적이 없으면 null입니다.
 * @property lastShownEpochDay 마지막으로 광고를 보여 준 날짜입니다.
 * @property shownCountOfDay [lastShownEpochDay]에 광고를 보여 준 횟수입니다.
 */
data class InterstitialAdRecord(
    val firstCalculatedAt: Long? = null,
    val calculationCountSinceLastAd: Int = 0,
    val lastShownAt: Long? = null,
    val lastShownEpochDay: Long? = null,
    val shownCountOfDay: Int = 0,
)
