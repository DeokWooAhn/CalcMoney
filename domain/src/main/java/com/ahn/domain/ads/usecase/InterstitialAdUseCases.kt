package com.ahn.domain.ads.usecase

import javax.inject.Inject

class InterstitialAdUseCases @Inject constructor(
    val recordCalculation: RecordInterstitialAdCalculationUseCase,
    val shouldShow: ShouldShowInterstitialAdUseCase,
    val recordShown: RecordInterstitialAdShownUseCase,
)
