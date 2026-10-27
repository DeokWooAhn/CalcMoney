package com.ahn.domain.ads.usecase

import com.ahn.domain.ads.model.InterstitialAdPolicy
import com.ahn.domain.ads.repository.InterstitialAdRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 지금 전면 광고를 보여 줄 차례인지 알려 줍니다. 기록은 바꾸지 않습니다.
 *
 * 광고가 실제로 떴을 때만 [RecordInterstitialAdShownUseCase]로 기록합니다. 광고를 아직 받아 오지 못해
 * 못 띄웠으면 조건이 그대로 남아 다음 기회에 다시 띄웁니다.
 */
class ShouldShowInterstitialAdUseCase internal constructor(
    private val repository: InterstitialAdRepository,
    private val clock: Clock,
) {
    @Inject
    constructor(repository: InterstitialAdRepository) : this(
        repository = repository,
        clock = Clock.systemDefaultZone(),
    )

    suspend operator fun invoke(): Boolean {
        return InterstitialAdPolicy.shouldShow(
            record = repository.getRecord(),
            now = clock.millis(),
            today = LocalDate.now(clock).toEpochDay(),
        )
    }
}
