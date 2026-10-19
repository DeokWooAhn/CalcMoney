package com.ahn.domain.review.usecase

import com.ahn.domain.review.model.ReviewPromptPolicy
import com.ahn.domain.review.repository.ReviewPromptRepository
import java.time.Clock
import javax.inject.Inject

/**
 * 계산 성공을 기록하고, 지금 리뷰를 요청해야 하는지 알려 줍니다.
 *
 * 요청해야 하면 요청 시각도 함께 남깁니다. 창이 실제로 떴는지는 OS·스토어가 알려 주지 않으므로
 * 요청을 보낸 시점을 기준으로 다음 요청 간격을 셉니다.
 */
class RecordCalculationSuccessUseCase internal constructor(
    private val repository: ReviewPromptRepository,
    private val clock: Clock,
) {
    @Inject
    constructor(repository: ReviewPromptRepository) : this(
        repository = repository,
        clock = Clock.systemDefaultZone(),
    )

    /** @return 지금 리뷰를 요청해야 하면 true입니다. */
    suspend operator fun invoke(): Boolean {
        val now = clock.millis()
        var shouldRequest = false

        repository.updateRecord { record ->
            val counted = record.copy(calculationSuccessCount = record.calculationSuccessCount + 1)
            shouldRequest = ReviewPromptPolicy.shouldRequest(counted, now)

            if (shouldRequest) counted.copy(lastRequestedAt = now) else counted
        }

        return shouldRequest
    }
}
