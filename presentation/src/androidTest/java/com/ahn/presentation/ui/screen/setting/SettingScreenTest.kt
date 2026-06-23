package com.ahn.presentation.ui.screen.setting

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ahn.domain.setting.model.ThemeMode
import com.ahn.presentation.ui.theme.CalcMoneyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun givenSettingScreen_whenThemeCardExpandedAndDarkSelected_thenCallsThemeSelection() {
        val selectedThemeModes = mutableListOf<ThemeMode>()

        composeRule.setSettingScreenContent(
            themeMode = ThemeMode.SYSTEM,
            onThemeModeSelected = selectedThemeModes::add,
        )

        composeRule.onNodeWithText("테마").performClick()
        composeRule.onNodeWithText("다크").performClick()

        assertEquals(listOf(ThemeMode.DARK), selectedThemeModes)
    }

    @Test
    fun givenSettingScreen_whenExchangeRateInfoExpanded_thenShowsDetailInformation() {
        composeRule.setSettingScreenContent(
            exchangeRateDateText = "2026.05.29",
            exchangeRateFetchedAtText = "2026.06.02 14:32",
        )

        composeRule.onNodeWithText("환율 기준 정보").performClick()

        composeRule.onNodeWithText("기준일").assertExists()
        composeRule.onNode(hasText("2026.05.29") and hasAnySibling(hasText("기준일"))).assertExists()
        composeRule.onNodeWithText("데이터 출처").assertExists()
        composeRule.onNodeWithText("한국수출입은행 Open API").assertExists()
        composeRule.onNodeWithText("마지막 갱신").assertExists()
        composeRule.onNodeWithText("2026.06.02 14:32").assertExists()
        composeRule.onNodeWithText(
            "환율 정보는 영업일 11시 전후 고시되는 기준 환율을 사용합니다.\n" +
                "주말·공휴일에는 새 환율이 제공되지 않을 수 있으며, 이 경우 직전 영업일 환율을 사용합니다.",
        ).assertExists()
        composeRule
            .onNodeWithText("제공되는 환율은 참고용이며 실제 거래 환율과 다를 수 있습니다.")
            .assertExists()
    }

    @Test
    fun givenSettingScreen_whenAppInfoProvided_thenShowsVersion() {
        composeRule.setSettingScreenContent(
            appInfo = AppInfo(versionName = "1.2.3"),
        )

        composeRule.onNodeWithText("버전").assertExists()
        composeRule.onNodeWithText("1.2.3").assertExists()
    }

    @Test
    fun givenSettingScreen_whenPrivacyPolicyClicked_thenCallsPrivacyPolicyAction() {
        var clickCount = 0

        composeRule.setSettingScreenContent(
            onPrivacyPolicyClick = { clickCount++ },
        )

        composeRule.onNodeWithText("개인정보 처리방침").performClick()

        assertEquals(1, clickCount)
    }

}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setSettingScreenContent(
    appInfo: AppInfo = AppInfo(versionName = "1.0"),
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    exchangeRateDateText: String? = null,
    exchangeRateFetchedAtText: String? = null,
    isExchangeRateLoading: Boolean = false,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    onPrivacyPolicyClick: () -> Unit = {},
) {
    setContent {
        CompositionLocalProvider(LocalInspectionMode provides true) {
            CalcMoneyTheme {
                SettingScreen(
                    appInfo = appInfo,
                    themeMode = themeMode,
                    exchangeRateDateText = exchangeRateDateText,
                    exchangeRateFetchedAtText = exchangeRateFetchedAtText,
                    isExchangeRateLoading = isExchangeRateLoading,
                    onThemeModeSelected = onThemeModeSelected,
                    onPrivacyPolicyClick = onPrivacyPolicyClick,
                )
            }
        }
    }
}
