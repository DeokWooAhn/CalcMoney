package com.ahn.presentation.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahn.presentation.R

/**
 * @param isRetrying 재시도를 포함해 관련 요청이 진행 중이면 true.
 *   버튼을 비활성화하고 진행 표시를 보여 준다.
 */
@Composable
fun LoadFailureMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isRetrying: Boolean = false,
    testTagName: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .then(if (testTagName != null) Modifier.testTag(testTagName) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        TextButton(
            onClick = onRetry,
            modifier = Modifier.testTag("failure.retry"),
            enabled = !isRetrying,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.exchange_rate_retry),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Preview
@Composable
private fun LoadFailureMessagePreview() {
    MaterialTheme {
        LoadFailureMessage(
            message = stringResource(R.string.favorite_rate_load_failed),
            onRetry = {},
        )
    }
}

@Preview
@Composable
private fun LoadFailureMessageRetryingPreview() {
    MaterialTheme {
        LoadFailureMessage(
            message = stringResource(R.string.favorite_rate_load_failed),
            onRetry = {},
            isRetrying = true,
        )
    }
}
