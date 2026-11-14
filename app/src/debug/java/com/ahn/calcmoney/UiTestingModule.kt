package com.ahn.calcmoney

import android.content.Context
import com.ahn.data.exchange.local.datasource.ExchangeRateLocalDataSource
import com.ahn.data.exchange.local.entity.ExchangeRateEntity
import com.ahn.presentation.main.UiTestingDataSetup
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 고정 환율은 디버그 APK에만 포함하고, 계산·즐겨찾기 저장은 실제 구현을 사용한다. */
@Module
@InstallIn(SingletonComponent::class)
object UiTestingModule {
    @Provides
    fun provideUiTestingDataSetup(
        local: ExchangeRateLocalDataSource,
        @ApplicationContext context: Context,
    ): UiTestingDataSetup = UiTestingDataSetup { useFixedRates ->
        withContext(Dispatchers.IO) {
            val marker = context.filesDir.resolve("maestro-fixed-rates")
            if (useFixedRates) {
                // 비정상 종료 뒤에도 일반 실행에서 테스트 캐시를 지울 수 있게 먼저 표시한다.
                marker.writeText("fixed")
                local.replaceRates(fixedRates())
            } else if (marker.exists()) {
                local.replaceRates(emptyList())
                check(marker.delete()) { "Failed to clear the fixed-rate test marker." }
            }
        }
    }

    private fun fixedRates(): List<ExchangeRateEntity> {
        val fetchedAt = System.currentTimeMillis()
        return listOf(
            ExchangeRateEntity("USD", "USD", "미국 달러", 1000.0, fetchedAt, "2026-10-15"),
            ExchangeRateEntity("AUD", "AUD", "호주 달러", 500.0, fetchedAt, "2026-10-15"),
        )
    }
}
