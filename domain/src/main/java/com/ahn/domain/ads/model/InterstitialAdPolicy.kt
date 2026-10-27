package com.ahn.domain.ads.model

import java.time.Duration

/**
 * 계산기를 떠날 때 전면 광고를 보여 줘도 되는지 정하는 규칙입니다.
 *
 * 처음 써 보는 날에는 띄우지 않고, 계산을 몇 번 해서 도움을 받은 뒤에만, 너무 자주 띄우지 않게 합니다.
 * 앱 코드에 실수가 있어도 과하게 뜨지 않도록 AdMob 콘솔의 광고 단위 게재빈도 제한도 함께 겁니다.
 * iOS `InterstitialAdPolicy`와 같은 값을 씁니다.
 */
object InterstitialAdPolicy {
    /** 계산에 처음 성공한 뒤 첫 광고까지 기다리는 시간입니다. */
    val GRACE_PERIOD: Duration = Duration.ofHours(24)

    /** 마지막 광고 뒤로 이만큼 계산에 성공해야 다음 광고를 보여 줍니다. */
    const val MIN_CALCULATION_COUNT = 4

    /** 광고 사이의 최소 간격입니다. */
    val MIN_INTERVAL: Duration = Duration.ofMinutes(5)

    /** 하루에 보여 주는 최대 횟수입니다. */
    const val MAX_SHOWN_PER_DAY = 3

    fun shouldShow(record: InterstitialAdRecord, now: Long, today: Long): Boolean {
        val firstCalculatedAt = record.firstCalculatedAt ?: return false

        val hasPassedGracePeriod = now - firstCalculatedAt >= GRACE_PERIOD.toMillis()
        val hasCalculatedEnough = record.calculationCountSinceLastAd >= MIN_CALCULATION_COUNT
        val hasIntervalPassed = record.lastShownAt
            ?.let { now - it >= MIN_INTERVAL.toMillis() }
            ?: true
        val shownCountToday = if (record.lastShownEpochDay == today) record.shownCountOfDay else 0
        val isUnderDailyLimit = shownCountToday < MAX_SHOWN_PER_DAY

        return hasPassedGracePeriod && hasCalculatedEnough && hasIntervalPassed && isUnderDailyLimit
    }
}
