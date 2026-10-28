package com.ahn.domain.ads.usecase

import com.ahn.domain.ads.repository.InterstitialAdRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 전면 광고를 보여 줬다고 기록합니다. 계산 횟수를 처음부터 다시 세고, 오늘 보여 준 횟수를 늘립니다.
 */
class RecordInterstitialAdShownUseCase internal constructor(
    private val repository: InterstitialAdRepository,
    private val clock: Clock,
) {
    @Inject
    constructor(repository: InterstitialAdRepository) : this(
        repository = repository,
        clock = Clock.systemDefaultZone(),
    )

    suspend operator fun invoke() {
        val now = clock.millis()
        val today = LocalDate.now(clock).toEpochDay()

        repository.updateRecord { record ->
            val shownCountToday = if (record.lastShownEpochDay == today) record.shownCountOfDay else 0

            record.copy(
                calculationCountSinceLastAd = 0,
                lastShownAt = now,
                lastShownEpochDay = today,
                shownCountOfDay = shownCountToday + 1,
            )
        }
    }
}
