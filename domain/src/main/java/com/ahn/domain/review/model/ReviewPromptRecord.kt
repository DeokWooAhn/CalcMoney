package com.ahn.domain.review.model

/**
 * 인앱 리뷰를 요청할 시점을 판단하는 데 쓰는 사용 기록입니다.
 *
 * 시각은 epoch milliseconds, 날짜는 기기 시간대 기준 epoch day입니다.
 *
 * @property firstOpenedAt 앱을 처음 연 시각입니다. 아직 기록이 없으면 null입니다.
 * @property lastOpenedEpochDay 앱을 마지막으로 연 날짜입니다. 같은 날 여러 번 열어도 하루로 셉니다.
 * @property openedDayCount 앱을 연 서로 다른 날짜 수입니다.
 * @property calculationSuccessCount 계산기에서 계산에 성공한 누적 횟수입니다.
 * @property lastRequestedAt 마지막으로 리뷰를 요청한 시각입니다. 요청한 적이 없으면 null입니다.
 */
data class ReviewPromptRecord(
    val firstOpenedAt: Long? = null,
    val lastOpenedEpochDay: Long? = null,
    val openedDayCount: Int = 0,
    val calculationSuccessCount: Int = 0,
    val lastRequestedAt: Long? = null,
)
