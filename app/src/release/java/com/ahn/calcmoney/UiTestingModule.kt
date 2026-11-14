package com.ahn.calcmoney

import com.ahn.presentation.main.UiTestingDataSetup
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 릴리스에는 고정 환율 데이터와 테스트 초기화 동작을 포함하지 않는다. */
@Module
@InstallIn(SingletonComponent::class)
object UiTestingModule {
    @Provides
    fun provideUiTestingDataSetup(): UiTestingDataSetup = UiTestingDataSetup { _ -> }
}
