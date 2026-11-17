package com.ahn.presentation.ui.component

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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoadFailureMessageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `재시도 중이 아니면 문구와 활성 버튼이 보이고 한 번 누르면 한 번 호출된다`() {
        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.exchange_rate_network_unavailable)
        var retryCount = 0
        composeRule.setContent {
            MaterialTheme {
                LoadFailureMessage(message = message, onRetry = { retryCount++ }, isRetrying = false)
            }
        }

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithTag("failure.retry").assertIsEnabled().performClick()
        composeRule.onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertCountEquals(0)
        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }

    @Test
    fun `재시도 중이면 버튼이 비활성이고 진행 표시가 보이며 눌러도 호출되지 않는다`() {
        val message = ApplicationProvider.getApplicationContext<Context>()
            .getString(R.string.exchange_rate_network_unavailable)
        var retryCount = 0
        composeRule.setContent {
            MaterialTheme {
                LoadFailureMessage(message = message, onRetry = { retryCount++ }, isRetrying = true)
            }
        }

        composeRule.onNodeWithTag("failure.retry").assertIsNotEnabled().performTouchInput { click() }
        composeRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, retryCount) }
    }
}
