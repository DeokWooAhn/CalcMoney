package com.ahn.domain.ads.usecase

import com.ahn.domain.ads.repository.InterstitialAdRepository
import java.time.Clock
import javax.inject.Inject

/**
 * 계산 성공을 전면 광고 기록에 셉니다. 처음 성공한 계산이면 그 시각도 남겨 유예 기간을 셉니다.
 */
class RecordInterstitialAdCalculationUseCase internal constructor(
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

        repository.updateRecord { record ->
            record.copy(
                firstCalculatedAt = record.firstCalculatedAt ?: now,
                calculationCountSinceLastAd = record.calculationCountSinceLastAd + 1,
            )
        }
    }
}
