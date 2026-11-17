package com.ahn.presentation.ui.screen.favorite

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.ahn.presentation.R
import com.ahn.presentation.ui.screen.exchange.ExchangeContract
import com.ahn.presentation.util.UiText
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavoriteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val exchangeState = ExchangeContract.State(
        favoriteCurrencyCodes = listOf("USD"),
        failure = ExchangeContract.Failure(
            scope = ExchangeContract.Failure.Scope.RATE,
            message = UiText.StringResource(R.string.exchange_rate_network_unavailable),
        ),
    )
    private val favoriteState = FavoriteContract.State()

    @Test
    fun `즐겨찾기 환율 조회가 실패하면 실패 문구가 보이고 재시도 인텐트 하나를 보낸다`() {
        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.exchange_rate_network_unavailable)
        val intents = mutableListOf<ExchangeContract.Intent>()
        composeRule.setContent {
            MaterialTheme {
                FavoriteScreen(
                    exchangeState = exchangeState,
                    favoriteState = favoriteState,
                    canRequestAds = false,
                    onExchangeIntent = { intents.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag("favorite.failure").assertIsDisplayed()
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(0)
        composeRule.onNodeWithTag("failure.retry").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(listOf(ExchangeContract.Intent.RefreshExchangeRates), intents) }
    }

    @Test
    fun `환율 새로고침 중이면 실패 표시가 남고 다시 시도 버튼이 비활성이며 진행 표시가 보인다`() {
        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.exchange_rate_network_unavailable)
        val intents = mutableListOf<ExchangeContract.Intent>()
        composeRule.setContent {
            MaterialTheme {
                // 다시 시도로 새로고침이 도는 동안: 환율 상태만 로딩이고 즐겨찾기 상태는 그대로다.
                FavoriteScreen(
                    exchangeState = exchangeState.copy(isLoading = true),
                    favoriteState = favoriteState,
                    canRequestAds = false,
                    onExchangeIntent = { intents.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag("favorite.failure").assertIsDisplayed()
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        composeRule.onNodeWithTag("failure.retry").assertIsNotEnabled().performTouchInput { click() }
        composeRule.runOnIdle { assertEquals(emptyList<ExchangeContract.Intent>(), intents) }
    }

    @Test
    fun `같은 상태에 실패가 없으면 재시도 없이 예비 문구가 보인다`() {
        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.favorite_rate_load_failed)
        val intents = mutableListOf<ExchangeContract.Intent>()
        composeRule.setContent {
            MaterialTheme {
                FavoriteScreen(
                    exchangeState = exchangeState.copy(failure = null),
                    favoriteState = favoriteState,
                    canRequestAds = false,
                    onExchangeIntent = { intents.add(it) },
                )
            }
        }

        composeRule.onNodeWithTag("favorite.failure").assertDoesNotExist()
        composeRule.onNodeWithTag("failure.retry").assertDoesNotExist()
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(emptyList<ExchangeContract.Intent>(), intents) }
    }
}
