package com.ahn.domain.review.usecase

import javax.inject.Inject

class ReviewPromptUseCases @Inject constructor(
    val recordAppOpen: RecordAppOpenUseCase,
    val recordCalculationSuccess: RecordCalculationSuccessUseCase,
)
