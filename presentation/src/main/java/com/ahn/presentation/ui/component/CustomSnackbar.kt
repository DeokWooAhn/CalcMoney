package com.ahn.presentation.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) { data ->
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            CustomWrapSnackbar(snackbarData = data)
        }
    }
}

@Composable
fun CustomWrapSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier,
    containColor: Color = Color.DarkGray,
    contentColor: Color = Color.White,
    cornerRadius: Int = 18,
) {
    Surface(
        modifier = modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .defaultMinSize(minHeight = 48.dp),
        shape = RoundedCornerShape(cornerRadius.dp),
        color = containColor,
        contentColor = contentColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = snackbarData.visuals.message,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val actionLabel = snackbarData.visuals.actionLabel
            if (actionLabel != null) {
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { snackbarData.performAction() }) {
                    Text(actionLabel)
                }
            }
        }
    }
}