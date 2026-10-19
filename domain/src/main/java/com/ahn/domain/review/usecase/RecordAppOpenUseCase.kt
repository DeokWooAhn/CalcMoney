package com.ahn.domain.review.usecase

import com.ahn.domain.review.repository.ReviewPromptRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 앱을 연 날짜를 기록합니다. 같은 날 여러 번 열어도 하루로 셉니다.
 */
class RecordAppOpenUseCase internal constructor(
    private val repository: ReviewPromptRepository,
    private val clock: Clock,
) {
    @Inject
    constructor(repository: ReviewPromptRepository) : this(
        repository = repository,
        clock = Clock.systemDefaultZone(),
    )

    suspend operator fun invoke() {
        val now = clock.millis()
        val today = LocalDate.now(clock).toEpochDay()

        repository.updateRecord { record ->
            if (record.lastOpenedEpochDay == today) {
                record
            } else {
                record.copy(
                    firstOpenedAt = record.firstOpenedAt ?: now,
                    lastOpenedEpochDay = today,
                    openedDayCount = record.openedDayCount + 1,
                )
            }
        }
    }
}
