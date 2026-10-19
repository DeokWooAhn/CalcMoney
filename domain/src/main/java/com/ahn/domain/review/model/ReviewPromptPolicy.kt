package com.ahn.domain.review.model

import java.time.Duration

/**
 * 인앱 리뷰를 요청해도 되는지 정하는 규칙입니다.
 *
 * 충분히 써 본 사용자에게만, 너무 자주 묻지 않게 요청합니다. 실제로 창을 띄울지는 OS·스토어가 다시 정하므로
 * (Play는 짧은 기간 안의 재요청을 무시하고 iOS는 1년에 3번까지만 띄웁니다) 이 규칙은 요청 횟수를 줄이는 쪽으로 잡습니다.
 * iOS `ReviewPromptPolicy`와 같은 값을 씁니다.
 */
object ReviewPromptPolicy {
    val MIN_TIME_SINCE_FIRST_OPEN: Duration = Duration.ofDays(3)
    const val MIN_OPENED_DAY_COUNT = 3
    const val MIN_CALCULATION_SUCCESS_COUNT = 5

    /** iOS의 1년 3회 제한과 맞춘 요청 간격입니다. */
    val REQUEST_INTERVAL: Duration = Duration.ofDays(120)

    fun shouldRequest(record: ReviewPromptRecord, now: Long): Boolean {
        val firstOpenedAt = record.firstOpenedAt ?: return false

        val hasUsedLongEnough = now - firstOpenedAt >= MIN_TIME_SINCE_FIRST_OPEN.toMillis()
        val hasOpenedOnEnoughDays = record.openedDayCount >= MIN_OPENED_DAY_COUNT
        val hasCalculatedEnough = record.calculationSuccessCount >= MIN_CALCULATION_SUCCESS_COUNT
        val hasIntervalPassed = record.lastRequestedAt
            ?.let { now - it >= REQUEST_INTERVAL.toMillis() }
            ?: true

        return hasUsedLongEnough && hasOpenedOnEnoughDays && hasCalculatedEnough && hasIntervalPassed
    }
}
