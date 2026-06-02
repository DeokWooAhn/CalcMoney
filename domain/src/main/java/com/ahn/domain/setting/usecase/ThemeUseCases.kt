package com.ahn.domain.setting.usecase

import javax.inject.Inject

class ThemeUseCases @Inject constructor(
    val getThemeMode: GetThemeModeUseCase,
    val saveThemeMode: SaveThemeModeUseCase,
)
